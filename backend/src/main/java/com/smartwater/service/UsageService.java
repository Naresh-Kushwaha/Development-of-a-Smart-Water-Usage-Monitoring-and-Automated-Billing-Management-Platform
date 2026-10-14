package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.Role;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UsageService {
    private final UsageRepository usage;
    private final HouseholdRepository households;
    private final AlertService alerts;

    @Transactional
    public UsageRes log(User actor, UsageReq r) {
        Household h;
        if (actor.getRole() == Role.ADMIN) {
            if (r.householdId() == null) throw ApiException.bad("householdId is required");
            h = households.findByIdAndApartmentId(r.householdId(), actor.getApartment().getId())
                    .orElseThrow(() -> ApiException.notFound("Household"));
        } else {
            h = actor.getHousehold();
            if (h == null) throw ApiException.bad("No household assigned to your account");
        }
        if (!h.hasMeter()) throw ApiException.bad("Unit " + h.getUnitNumber() + " has no water meter configured");
        if (r.date().isAfter(LocalDate.now())) throw ApiException.bad("Reading date cannot be in the future");
        if (usage.existsByHouseholdIdAndReadingDate(h.getId(), r.date()))
            throw ApiException.conflict("A reading for " + r.date() + " already exists for unit " + h.getUnitNumber());
        WaterUsageLog l = save(h, r.date(), r.litres(), "MANUAL");
        return new UsageRes(l.getReadingDate(), l.getLitres(), l.getSource());
    }

    public List<UsageRes> mine(User u, LocalDate from, LocalDate to) {
        if (u.getHousehold() == null) throw ApiException.bad("No household assigned to your account");
        return usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(u.getHousehold().getId(), from, to)
                .stream().map(l -> new UsageRes(l.getReadingDate(), l.getLitres(), l.getSource())).toList();
    }

    /** CSV columns: unit_number, date (yyyy-MM-dd), litres. Invalid rows are reported, duplicates skipped. */
    @Transactional
    public CsvResult importCsv(Long apartmentId, MultipartFile file) {
        if (file.isEmpty()) throw ApiException.bad("Empty file");
        int imported = 0, duplicates = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        Map<String, Household> byUnit = new HashMap<>();
        households.findByApartmentIdOrderByUnitNumber(apartmentId).forEach(h -> byUnit.put(h.getUnitNumber().toLowerCase(), h));

        CSVFormat fmt = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setTrim(true).setIgnoreEmptyLines(true).build();
        try (CSVParser p = fmt.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (String col : List.of("unit_number", "date", "litres"))
                if (!p.getHeaderMap().containsKey(col)) throw ApiException.bad("Missing CSV column: " + col + " (expected unit_number,date,litres)");
            for (CSVRecord rec : p) {
                long row = rec.getRecordNumber() + 1;
                try {
                    String unit = rec.get("unit_number");
                    LocalDate date = LocalDate.parse(rec.get("date"));
                    BigDecimal litres = new BigDecimal(rec.get("litres"));
                    Household h = byUnit.get(unit.toLowerCase());
                    if (h == null) { errors.add("Row " + row + ": unknown unit '" + unit + "'"); continue; }
                    if (!h.hasMeter()) { errors.add("Row " + row + ": unit " + unit + " has no meter"); continue; }
                    if (litres.signum() < 0) { errors.add("Row " + row + ": litres must be >= 0"); continue; }
                    if (date.isAfter(LocalDate.now())) { errors.add("Row " + row + ": date is in the future"); continue; }
                    String key = h.getId() + "|" + date;
                    if (!seenInFile.add(key) || usage.existsByHouseholdIdAndReadingDate(h.getId(), date)) { duplicates++; continue; }
                    save(h, date, litres, "CSV");
                    imported++;
                } catch (RuntimeException e) {
                    errors.add("Row " + row + ": " + (e instanceof ApiException ? e.getMessage() : "invalid value (" + e.getMessage() + ")"));
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.bad("Could not read CSV file");
        }
        if (imported > 0) alerts.check(apartmentId);   // evaluate thresholds/anomalies on fresh data
        return new CsvResult(imported, duplicates, errors);
    }

    public List<UnitUsage> comparison(Long apartmentId, LocalDate from, LocalDate to) {
        Map<Long, BigDecimal> litres = new HashMap<>();
        for (Object[] row : usage.totalsByHousehold(apartmentId, from, to)) litres.put((Long) row[0], (BigDecimal) row[1]);
        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1);
        return households.findByApartmentIdOrderByUnitNumber(apartmentId).stream().map(h -> {
            BigDecimal l = litres.getOrDefault(h.getId(), BigDecimal.ZERO);
            BigDecimal perPerson = l.divide(BigDecimal.valueOf(days * h.getOccupants()), 1, RoundingMode.HALF_UP);
            return new UnitUsage(h.getUnitNumber(), l.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP),
                    h.getAreaSqft(), h.getOccupants(), perPerson);
        }).toList();
    }

    private WaterUsageLog save(Household h, LocalDate date, BigDecimal litres, String source) {
        WaterUsageLog l = new WaterUsageLog();
        l.setHousehold(h); l.setReadingDate(date); l.setLitres(litres); l.setSource(source);
        return usage.save(l);
    }
}
