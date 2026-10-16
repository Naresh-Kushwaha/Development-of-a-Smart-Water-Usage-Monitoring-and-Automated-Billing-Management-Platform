package com.smartwater.web;

import com.smartwater.domain.Enums.Role;
import com.smartwater.domain.Invoice;
import com.smartwater.domain.User;
import com.smartwater.repo.InvoiceRepository;
import com.smartwater.security.Current;
import com.smartwater.service.PdfService;
import com.smartwater.web.Dtos.InvoiceRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceRepository invoices;
    private final PdfService pdf;

    @GetMapping
    public List<InvoiceRes> mine() {
        User u = Current.user();
        if (u.getHousehold() == null) return List.of();
        return invoices.findByHouseholdIdOrderByCreatedAtDesc(u.getHousehold().getId()).stream().map(Mapper::invoice).toList();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        User u = Current.user();
        Invoice inv = invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice"));
        boolean allowed = u.getRole() == Role.ADMIN
                ? inv.getHousehold().getApartment().getId().equals(u.getApartment().getId())
                : u.getHousehold() != null && u.getHousehold().getId().equals(inv.getHousehold().getId());
        if (!allowed) throw ApiException.notFound("Invoice");   // 404 rather than 403: don't reveal existence
        String name = "invoice-" + inv.getHousehold().getUnitNumber() + "-" + inv.getBillingCycle().getLabel().replaceAll("[^A-Za-z0-9]+", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.render(inv));
    }
}
