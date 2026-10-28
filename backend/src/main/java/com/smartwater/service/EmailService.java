package com.smartwater.service;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
   private final SpringTemplateEngine templateEngine;
    public EmailService(JavaMailSender sender, @Value("${app.mail.enabled}") boolean enabled,
                        @Value("${app.mail.from}") String from) {
        this.sender = sender; this.enabled = enabled; this.from = from;
        this.templateEngine = new SpringTemplateEngine();
    }

   
public void send(String to, String subject, String body) {

    if (!enabled) {
        log.info("[mail disabled] to={} subject={}", to, subject);
        return;
    }

    try {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("body", body);

        String html = templateEngine.process(
                "email/water-alert",
                context
        );

        MimeMessage message = sender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                false,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        sender.send(message);

    } catch (Exception e) {
        log.warn(
                "Could not send email to {}: {}",
                to,
                e.getMessage()
        );
    }
}


    public void sendWaterUsageAlert(
            String recipient,
            String customerName,
            double currentUsageLitres,
            double dailyLimitLitres) {

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email is required");
        }

        if (!Double.isFinite(currentUsageLitres)
                || !Double.isFinite(dailyLimitLitres)
                || currentUsageLitres < 0
                || dailyLimitLitres < 0) {
            throw new IllegalArgumentException(
                    "Water usage values must be valid and non-negative");
        }

        if (currentUsageLitres <= dailyLimitLitres) {
            throw new IllegalArgumentException(
                    "Usage must exceed the daily limit to send an alert");
        }

        double excess = currentUsageLitres - dailyLimitLitres;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(
                "Water Usage Alert - High Consumption Detected");

        message.setText(
                "Hello " + safeName(customerName) + ",\n\n"
                + "Your water consumption has exceeded your "
                + "configured daily limit.\n\n"
                + "Current usage: "
                + currentUsageLitres + " litres\n"
                + "Daily limit: "
                + dailyLimitLitres + " litres\n"
                + "Excess usage: "
                + excess + " litres\n\n"
                + "Please check your water consumption and "
                + "inspect your plumbing for possible leaks.\n\n"
                + "Regards,\nSmart Water Platform");

        try {
            sender.send(message);
            log.info("Water usage alert submitted for delivery");
        } catch (MailException ex) {
            log.error("Failed to submit water usage alert: {}", ex.getMessage());
            throw ex;
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "User";
        }

        // Avoid unexpected multiline text in the greeting.
        return name.replace("\r", " ").replace("\n", " ");
    }
}
