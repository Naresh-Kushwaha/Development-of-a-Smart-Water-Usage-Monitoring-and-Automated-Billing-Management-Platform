package com.smartwater.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
@Service
@Slf4j
public class EmailService {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    private final SpringTemplateEngine templateEngine;

    @Value("${app.public-url:http://localhost:5173}")
    private String appUrl;
    public EmailService(JavaMailSender sender, @Value("${app.mail.enabled}") boolean enabled,
                        @Value("${app.mail.from}") String from, SpringTemplateEngine templateEngine) {
        this.sender = sender; this.enabled = enabled; this.from = from;
        this.templateEngine = templateEngine;
    }

    /** Best-effort delivery: mail problems must never break billing or alert flows. */
    public void send(String to, String subject, String body) {
        try {
            deliver(to, subject, body);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }

    public void deliver(String to, String subject, String body) {

        if (!enabled) {
            log.info("[mail disabled] to={} subject={}", to, subject);
            return;
        }

        try {
            // Prepare Thymeleaf context
            Context context = new Context();

            context.setVariable("subject", subject);
            context.setVariable("body", body);
            context.setVariable("appUrl", appUrl);

            // Render HTML template
            String html = templateEngine.process(
                    "notification",
                    context
            );

            // Create MIME message
            MimeMessage message = sender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            // true means HTML content
            helper.setText(html, true);

            // Send email
            sender.send(message);

            log.info("HTML email successfully sent to={}", to);

        } catch (MessagingException e) {

            log.error("Failed to prepare email for {}", to, e);

            throw new IllegalStateException(
                    "Failed to prepare HTML email", e
            );

        } catch (Exception e) {

            log.error("Failed to send email to {}", to, e);

            throw new IllegalStateException(
                    "Failed to deliver email", e
            );
        }
    }
}
