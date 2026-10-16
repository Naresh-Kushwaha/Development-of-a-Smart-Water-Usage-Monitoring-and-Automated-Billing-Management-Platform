package com.smartwater.service;

import com.smartwater.domain.Invoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

@Service
public class PdfService {
    private final String paymentInstructions;

    public PdfService(@Value("${app.payment-instructions}") String paymentInstructions) {
        this.paymentInstructions = paymentInstructions;
    }

    public byte[] render(Invoice inv) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDFont bold = PDType1Font.HELVETICA_BOLD, reg = PDType1Font.HELVETICA;
                var cycle = inv.getBillingCycle();
                var h = inv.getHousehold();
                float y = 790;
                y = line(cs, bold, 22, 50, y, "Water Usage Invoice");
                y = line(cs, reg, 11, 50, y, h.getApartment().getName());
                y -= 10;
                y = line(cs, reg, 11, 50, y, "Invoice #" + inv.getId() + "   |   Status: " + inv.getStatus());
                y = line(cs, reg, 11, 50, y, "Unit: " + h.getUnitNumber() + "   |   Area: " + h.getAreaSqft() + " sq ft   |   Occupants: " + h.getOccupants());
                y = line(cs, reg, 11, 50, y, "Billing cycle: " + cycle.getLabel() + " (" + cycle.getStartDate() + " to " + cycle.getEndDate() + ")");
                y -= 16;
                y = line(cs, bold, 13, 50, y, "Charges");
                y = row(cs, reg, y, "Metered consumption", String.format(Locale.US, "%.3f kL", inv.getConsumptionKl()));
                y = row(cs, reg, y, "Water charge (tariff / cost share)", money(inv.getBaseCharge()));
                y = row(cs, reg, y, "Shared-area allocation (garden, pool, lobby)", money(inv.getSharedCharge()));
                if (inv.getAdjustment().signum() != 0)
                    y = row(cs, reg, y, "Adjustment" + (inv.getAdjustmentNote() == null ? "" : " - " + inv.getAdjustmentNote()), money(inv.getAdjustment()));
                y -= 6;
                y = row(cs, bold, y, "TOTAL DUE", money(inv.getTotal()));
                y -= 20;
                y = line(cs, bold, 13, 50, y, "Payment instructions");
                for (String l : wrap(paymentInstructions, 90)) y = line(cs, reg, 11, 50, y, l);
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String money(java.math.BigDecimal v) { return String.format(Locale.US, "INR %,.2f", v); }

    private static float row(PDPageContentStream cs, PDFont f, float y, String label, String value) throws IOException {
        line(cs, f, 11, 50, y, label);
        return line(cs, f, 11, 420, y, value);
    }

    private static float line(PDPageContentStream cs, PDFont f, float size, float x, float y, String s) throws IOException {
        cs.beginText();
        cs.setFont(f, size);
        cs.newLineAtOffset(x, y);
        cs.showText(clean(s));
        cs.endText();
        return y - size * 1.6f;
    }

    /** Standard PDF fonts only cover Latin-1; replace anything else so showText never throws. */
    private static String clean(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) b.append(c >= 32 && c < 256 ? c : '?');
        return b.toString();
    }

    private static java.util.List<String> wrap(String text, int width) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split("\\s+")) {
            if (cur.length() + w.length() + 1 > width) { lines.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
