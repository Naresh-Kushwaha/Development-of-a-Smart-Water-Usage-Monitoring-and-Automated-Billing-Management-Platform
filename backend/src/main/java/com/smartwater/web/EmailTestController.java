package com.smartwater.web;

import com.smartwater.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.MailException;

@RestController
@RequestMapping("/api/test-email")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    public record AlertTestRequest(
            String to,
            String name,
            double currentUsageLitres,
            double dailyLimitLitres) {
    }

    @PostMapping("/water-alert")
    public ResponseEntity<String> testWaterAlert(
            @RequestBody AlertTestRequest request) {

        try {
            emailService.sendWaterUsageAlert(
                    request.to(),
                    request.name(),
                    request.currentUsageLitres(),
                    request.dailyLimitLitres());

            return ResponseEntity.ok(
                    "Water alert submitted to the SMTP server");

        } catch (MailException ex) {
            return ResponseEntity.internalServerError().body(
                    "Unable to submit the email");
        }
    }
}
