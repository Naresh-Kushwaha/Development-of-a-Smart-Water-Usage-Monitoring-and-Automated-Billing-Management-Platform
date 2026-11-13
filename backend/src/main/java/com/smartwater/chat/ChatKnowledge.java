package com.smartwater.chat;

import com.smartwater.domain.Enums.Role;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The platform's "help centre", loaded once from src/main/resources/chatbot/*.md.
 * To teach the assistant something new, just edit those files - no code change needed.
 */
@Component
public class ChatKnowledge {
    private final String resident;
    private final String admin;

    public ChatKnowledge() {
        this.resident = read("chatbot/knowledge-resident.md");
        this.admin = read("chatbot/knowledge-admin.md");
    }

    /** Admins also see the resident pages, so they get both documents. */
    public String forRole(Role role) {
        return role == Role.ADMIN ? admin + "\n\n" + resident : resident;
    }

    private static String read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing chatbot knowledge file: " + path, e);
        }
    }
}
