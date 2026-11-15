package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.Role;
import com.smartwater.events.DomainEvents;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UsageService {
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,           // 2026-10-12
            DateTimeFormatter.ofPattern("d-M-uuuu"),    // 12-10-2026
            DateTimeFormatter.ofPattern("d/M/uuuu"));   // 12/10/2026

    private final UsageRepository usage;
    private final HouseholdRepository households;
    private final DomainEvents events;

    @Transactional
    @CacheEvict(cacheNames = {"dashboard", "compare"}, allEntries = true)
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
        events.usageIngested(h.getApartment().getId());      // overuse/leak check runs after commit, off the request path
        return new UsageRes(l.getReadingDate(), l.getLitres(), l.getSource());
    }

    public List<UsageRes> mine(User u, LocalDate from, LocalDate to) {
        if (u.getHousehold() == null) throw ApiException.bad("No household assigned to your account");
        return usage.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(u.getHousehold().getId(), from, to)
                .stream().map(l -> new UsageRes(l.getReadingDate(), l.getLitres(), l.getSource())).toList();
    }

    /**
     * CSV columns: unit_number, date, litres. Tolerates Excel quirks: UTF-8 BOM, comma/semicolon/tab delimiters,
     * header names like "Unit Number", dates as yyyy-MM-dd or dd-MM-yyyy, and blank rows.
     * Invalid rows are reported, duplicates skipped.
     */
    @Transactional
    @CacheEvict(cacheNames = {"dashboard", "compare"}, allEntries = true)
    public CsvResult importCsv(Long apartmentId, MultipartFile file) {
        if (file.isEmpty()) throw ApiException.bad("Empty file");
        int imported = 0, duplicates = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        Map<String, Household> byUnit = new HashMap<>();
        households.findByApartmentIdOrderByUnitNumber(apartmentId).forEach(h -> byUnit.put(h.getUnitNumber().toLowerCase(), h));

        String text;
        try { text = new String(file.getBytes(), StandardCharsets.UTF_8); }
        catch (IOException e) { throw ApiException.bad("Could not read CSV file"); }
        if (text.startsWith("\uFEFF")) text = text.substring(1);            // Excel's byte-order mark

        String firstLine = text.lines().findFirst().orElse("");             // pick the delimiter used in the header
        char delimiter = ',';
        long best = -1;
        for (char d : new char[]{',', ';', '\t'}) {
            long c = firstLine.chars().filter(ch -> ch == d).count();
            if (c > best) { best = c; delimiter = d; }
        }

        CSVFormat fmt = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setTrim(true).setIgnoreEmptyLines(true).build();
        try (CSVParser p = fmt.parse(new StringReader(text))) {
            Iterator<CSVRecord> it = p.iterator();
            if (!it.hasNext()) throw ApiException.bad("Empty file");

            CSVRecord head = it.next();
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < head.size(); i++)
                idx.put(head.get(i).trim().toLowerCase().replaceAll("[\\s\\-]+", "_"), i);
            for (String col : List.of("unit_number", "date", "litres"))
                if (!idx.containsKey(col))
                    throw ApiException.bad("Missing CSV column: " + col + ". Found: " + idx.keySet()
                            + " (expected unit_number,date,litres)");

            while (it.hasNext()) {
                CSVRecord rec = it.next();
                long row = rec.getRecordNumber();
                if (rec.stream().allMatch(String::isBlank)) continue;       // ",," rows left behind by Excel
                try {
                    String unit = cell(rec, idx, "unit_number");
                    LocalDate date = parseDate(cell(rec, idx, "date"));
                    BigDecimal litres = new BigDecimal(cell(rec, idx, "litres"));
                    Household h = byUnit.get(unit.toLowerCase());
                    if (h == null) { errors.add("Row " + row + ": unknown unit '" + unit + "'"); continue; }
                    if (!h.hasMeter()) { errors.add("Row " + row + ": unit " + unit + " has no meter"); continue; }
                    if (litres.signum() < 0) { errors.add("Row " + row + ": litres must be >= 0"); continue; }
                    if (date.isAfter(LocalDate.now())) { errors.add("Row " + row + ": date is in the future"); continue; }
                    String key = h.getId() + "|" + date;
                    if (!seenInFile.add(key) || usage.existsByHouseholdIdAndReadingDate(h.getId(), date)) { duplicates++; continue; }
                    save(h, date, litres, "CSV");
                    imported++;
                } catch (ApiException e) {
                    errors.add("Row " + row + ": " + e.getMessage());
                } catch (RuntimeException e) {
                    errors.add("Row " + row + ": invalid value (" + e.getMessage() + ")");
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.bad("Could not read CSV file");
        }
        if (imported > 0) events.usageIngested(apartmentId);   // evaluate thresholds/anomalies once the import has committed
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

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) throw ApiException.bad("date is empty");
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(s.trim(), f); } catch (DateTimeParseException ignored) { }
        }
        throw ApiException.bad("date '" + s + "' not recognised (use yyyy-MM-dd or dd-MM-yyyy)");
    }

    private static String cell(CSVRecord rec, Map<String, Integer> idx, String col) {
        int i = idx.get(col);
        return i < rec.size() ? rec.get(i) : "";
    }

    private WaterUsageLog save(Household h, LocalDate date, BigDecimal litres, String source) {
        WaterUsageLog l = new WaterUsageLog();
        l.setHousehold(h); l.setReadingDate(date); l.setLitres(litres); l.setSource(source);
        return usage.save(l);
    }
}
