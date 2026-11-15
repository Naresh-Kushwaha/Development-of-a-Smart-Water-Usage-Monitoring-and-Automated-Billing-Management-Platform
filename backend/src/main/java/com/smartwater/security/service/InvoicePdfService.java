
package com.smartwater.service;

import com.smartwater.domain.Invoice;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;

    public byte[] render(Invoice invoice, String name) {

        Context context = new Context();

        context.setVariable("invoice", invoice);

        context.setVariable(
                "formattedTotal",
                formatCurrency(invoice.getTotal())
        );

        context.setVariable(
                "formattedBaseCharge",
                formatCurrency(invoice.getBaseCharge())
        );

        context.setVariable(
                "formattedSharedCharge",
                formatCurrency(invoice.getSharedCharge())
        );

        context.setVariable(
                "formattedAdjustment",
                formatCurrency(invoice.getAdjustment())
        );

        context.setVariable(
                "formattedConsumption",
                invoice.getConsumptionKl().toPlainString()
        );

        context.setVariable(
                "apartmentName",
                invoice.getHousehold()
                        .getApartment()
                        .getName()
        );

        String html = templateEngine.process(
                "invoice",
                context
        );

        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();

            return output.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate invoice PDF", e
            );
        }
    }

    private String formatCurrency(BigDecimal amount) {

        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("en", "IN")
                );

        return formatter.format(
                amount != null ? amount : BigDecimal.ZERO
        );
    }
}
