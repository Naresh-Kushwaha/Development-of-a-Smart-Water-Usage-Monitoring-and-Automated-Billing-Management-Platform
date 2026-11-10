package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.Role;
import com.smartwater.events.DomainEvents;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.smartwater.TestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {
    @Mock UsageRepository usage;
    @Mock HouseholdRepository households;
    @Mock DomainEvents events;
    @InjectMocks UsageService service;

    private final Apartment apt = apartment(1L);
    private final Household metered = household(10L, apt, "A-101", "900", 3, "M-1");
    private final Household unmetered = household(11L, apt, "A-102", "700", 2, null);
    private final LocalDate yesterday = LocalDate.now().minusDays(1);

    @BeforeEach
    void apartmentUnits() {
        lenient().when(households.findByApartmentIdOrderByUnitNumber(1L)).thenReturn(List.of(metered, unmetered));
    }

    private static MockMultipartFile csv(String body) {
        return new MockMultipartFile("file", "u.csv", "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    // ================= manual logging =================
    @Test
    void log_residentAlwaysLogsToOwnHousehold_evenIfAnotherIdIsSent() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, metered);
        when(usage.save(any())).thenAnswer(i -> i.getArgument(0));

        UsageRes res = service.log(resident, new UsageReq(999L, yesterday, bd("120")));

        ArgumentCaptor<WaterUsageLog> cap = ArgumentCaptor.forClass(WaterUsageLog.class);
        verify(usage).save(cap.capture());
        assertEquals(10L, cap.getValue().getHousehold().getId());
        assertEquals("MANUAL", res.source());
        assertEquals(bd("120"), res.litres());
        verifyNoInteractions(households);
        verify(events).usageIngested(1L);                 // a manual reading also triggers the alert check
    }

    @Test
    void log_residentWithoutHouseholdIsRejected() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, null);
        var ex = assertThrows(ApiException.class, () -> service.log(resident, new UsageReq(null, yesterday, bd("1"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void log_rejectsFutureDates() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, metered);
        var ex = assertThrows(ApiException.class,
                () -> service.log(resident, new UsageReq(null, LocalDate.now().plusDays(1), bd("100"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(usage, never()).save(any());
    }

    @Test
    void log_rejectsUnmeteredHousehold() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, unmetered);
        var ex = assertThrows(ApiException.class, () -> service.log(resident, new UsageReq(null, yesterday, bd("100"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void log_duplicateReadingGives409() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, metered);
        when(usage.existsByHouseholdIdAndReadingDate(10L, yesterday)).thenReturn(true);

        var ex = assertThrows(ApiException.class, () -> service.log(resident, new UsageReq(null, yesterday, bd("100"))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(usage, never()).save(any());
    }

    @Test
    void log_adminMustSupplyHouseholdId() {
        User admin = user(2L, "a@x.com", Role.ADMIN, apt, null);
        var ex = assertThrows(ApiException.class, () -> service.log(admin, new UsageReq(null, yesterday, bd("100"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void log_adminCannotLogForAnotherApartment() {
        User admin = user(2L, "a@x.com", Role.ADMIN, apt, null);
        when(households.findByIdAndApartmentId(50L, 1L)).thenReturn(Optional.empty());
        var ex = assertThrows(ApiException.class, () -> service.log(admin, new UsageReq(50L, yesterday, bd("100"))));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void log_adminCanLogForHouseholdInOwnApartment() {
        User admin = user(2L, "a@x.com", Role.ADMIN, apt, null);
        when(households.findByIdAndApartmentId(10L, 1L)).thenReturn(Optional.of(metered));
        when(usage.save(any())).thenAnswer(i -> i.getArgument(0));

        UsageRes res = service.log(admin, new UsageReq(10L, yesterday, bd("250")));

        assertEquals(yesterday, res.date());
    }

    // ================= my readings =================
    @Test
    void mine_requiresAHousehold() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, null);
        var ex = assertThrows(ApiException.class, () -> service.mine(resident, yesterday.minusDays(5), yesterday));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void mine_returnsMappedReadings() {
        User resident = user(1L, "r@x.com", Role.RESIDENT, apt, metered);
        WaterUsageLog l = new WaterUsageLog();
        l.setHousehold(metered); l.setReadingDate(yesterday); l.setLitres(bd("300")); l.setSource("CSV");
        when(usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(10L, yesterday.minusDays(5), yesterday))
                .thenReturn(List.of(l));

        List<UsageRes> out = service.mine(resident, yesterday.minusDays(5), yesterday);

        assertEquals(1, out.size());
        assertEquals("CSV", out.get(0).source());
    }

    // ================= CSV import =================
    @Test
    void importCsv_importsValidRowsAndRunsAlertCheck() {
        when(usage.save(any())).thenAnswer(i -> i.getArgument(0));

        CsvResult res = service.importCsv(1L, csv("unit_number,date,litres\nA-101," + yesterday + ",410\n"));

        assertEquals(1, res.imported());
        assertEquals(0, res.duplicates());
        assertTrue(res.errors().isEmpty());
        verify(events).usageIngested(1L);
    }

    @Test
    void importCsv_skipsReadingsAlreadyInDatabase() {
        when(usage.existsByHouseholdIdAndReadingDate(10L, yesterday)).thenReturn(true);

        CsvResult res = service.importCsv(1L, csv("unit_number,date,litres\nA-101," + yesterday + ",410\n"));

        assertEquals(0, res.imported());
        assertEquals(1, res.duplicates());
        verify(usage, never()).save(any());
        verify(events, never()).usageIngested(any());
    }

    @Test
    void importCsv_skipsDuplicateRowsInsideTheSameFile() {
        when(usage.save(any())).thenAnswer(i -> i.getArgument(0));

        CsvResult res = service.importCsv(1L,
                csv("unit_number,date,litres\nA-101," + yesterday + ",410\nA-101," + yesterday + ",999\n"));

        assertEquals(1, res.imported());
        assertEquals(1, res.duplicates());
    }

    @Test
    void importCsv_reportsEachKindOfBadRow_andContinues() {
        String body = "unit_number,date,litres\n"
                + "Z-9," + yesterday + ",100\n"                        // unknown unit
                + "A-102," + yesterday + ",100\n"                      // unit without meter
                + "A-101," + yesterday + ",-5\n"                       // negative litres
                + "A-101," + LocalDate.now().plusDays(2) + ",100\n";   // future date

        CsvResult res = service.importCsv(1L, csv(body));

        assertEquals(0, res.imported());
        assertEquals(4, res.errors().size());
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("unknown unit")));
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("has no meter")));
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("must be >= 0")));
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("in the future")));
        verify(usage, never()).save(any());
        verify(events, never()).usageIngested(any());
    }

    @Test
    void importCsv_reportsNonNumericLitres() {
        CsvResult res = service.importCsv(1L, csv("unit_number,date,litres\nA-101," + yesterday + ",abc\n"));
        assertEquals(1, res.errors().size());
        assertTrue(res.errors().get(0).contains("invalid value"));
    }

    @Test
    void importCsv_missingRequiredColumnIsRejected() {
        var ex = assertThrows(ApiException.class,
                () -> service.importCsv(1L, csv("unit,date,litres\nA-101," + yesterday + ",410\n")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Missing CSV column"));
    }

    @Test
    void importCsv_emptyFileIsRejected() {
        var empty = new MockMultipartFile("file", "e.csv", "text/csv", new byte[0]);
        var ex = assertThrows(ApiException.class, () -> service.importCsv(1L, empty));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ================= admin comparison =================
    @Test
    void comparison_convertsToKilolitresAndPerPersonPerDay() {
        LocalDate from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 10);   // 10 days
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, bd("30000")});                                       // 30 kL, 3 people
        when(usage.totalsByHousehold(1L, from, to)).thenReturn(rows);

        List<UnitUsage> out = service.comparison(1L, from, to);

        assertEquals(2, out.size());
        assertEquals("A-101", out.get(0).unitNumber());
        assertEquals(0, bd("30.00").compareTo(out.get(0).kl()));
        assertEquals(0, bd("1000.0").compareTo(out.get(0).litresPerPersonPerDay()));   // 30000 / (10 days * 3 people)
        assertEquals(0, BigDecimal.ZERO.compareTo(out.get(1).kl()));                   // household with no readings
    }
}
