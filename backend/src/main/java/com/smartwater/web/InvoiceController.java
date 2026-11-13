
package com.smartwater.web;

import com.smartwater.domain.Enums;
import com.smartwater.domain.Invoice;
import com.smartwater.domain.User;
import com.smartwater.repo.InvoiceRepository;
import com.smartwater.service.InvoicePdfService;

import com.smartwater.security.Current;

import com.smartwater.service.PdfService;
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
    private final InvoicePdfService invoicePdfService;


    @GetMapping
    public List<Dtos.InvoiceRes> mine() {
        User u = Current.user();
        if (u.getHousehold() == null) return List.of();
        return invoices.findByHouseholdIdOrderByCreatedAtDesc(u.getHousehold().getId()).stream().map(Mapper::invoice).toList();
    }


    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {

        User u = Current.user();

        Invoice inv = invoices.findById(id)
                .orElseThrow(() -> ApiException.notFound("Invoice"));

        boolean allowed = u.getRole() == Enums.Role.ADMIN
                ? inv.getHousehold().getApartment()
                .getId().equals(u.getApartment().getId())
                : u.getHousehold() != null
                && u.getHousehold().getId()
                .equals(inv.getHousehold().getId());

        if (!allowed) {
            throw ApiException.notFound("Invoice");
        }

        String name = "invoice-" +
                inv.getHousehold().getUnitNumber() + "-" +
                inv.getBillingCycle().getLabel()
                        .replaceAll("[^A-Za-z0-9]+", "_") +
                ".pdf";

        byte[] pdfBytes = invoicePdfService.render(
                inv,
                inv.getHousehold().getApartment().getName()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
