package com.smartwater.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender sender, @Value("${app.mail.enabled}") boolean enabled,
                        @Value("${app.mail.from}") String from) {
        this.sender = sender; this.enabled = enabled; this.from = from;
    }

    /** Best-effort delivery: mail problems must never break billing or alert flows. */
    public void send(String to, String subject, String body) {
        if (!enabled) { log.info("[mail disabled] to={} subject={}", to, subject); return; }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body);
            sender.send(m);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }
}
