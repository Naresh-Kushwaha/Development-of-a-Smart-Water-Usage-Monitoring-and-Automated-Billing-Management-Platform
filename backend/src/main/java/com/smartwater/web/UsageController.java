package com.smartwater.web;

import com.smartwater.security.Current;
import com.smartwater.service.UsageService;
import com.smartwater.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UsageController {
    private final UsageService usage;

    @PostMapping("/api/usage")
    public UsageRes log(@Valid @RequestBody UsageReq r) { return usage.log(Current.user(), r); }

    @GetMapping("/api/usage")
    public List<UsageRes> mine(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return usage.mine(Current.user(), from, to);
    }

    @PostMapping("/api/admin/usage/upload")
    public CsvResult upload(@RequestParam("file") MultipartFile file) {
        return usage.importCsv(Current.apartmentId(), file);
    }

    @GetMapping("/api/admin/usage/comparison")
    public List<UnitUsage> comparison(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return usage.comparison(Current.apartmentId(), from, to);
    }
}
