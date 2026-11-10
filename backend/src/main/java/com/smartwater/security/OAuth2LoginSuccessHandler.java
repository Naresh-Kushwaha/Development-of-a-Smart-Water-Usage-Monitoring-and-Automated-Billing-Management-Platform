package com.smartwater.security;

import com.smartwater.domain.User;
import com.smartwater.repo.UserRepository;
import com.smartwater.store.KeyValueStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs after Google confirms who the person is. We then either
 *  - sign them in (an account with that VERIFIED email exists) -> redirect to the SPA with our own JWT, or
 *  - park their email/name under a short-lived ticket in Redis and ask the SPA for apartment code + unit,
 *    because every account in this system belongs to an apartment.
 * Google is only used to prove the email; the session it created is thrown away.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    public static final String TICKET_PREFIX = "oauth:signup:";
    private static final Duration TICKET_TTL = Duration.ofMinutes(10);

    private final UserRepository users;
    private final JwtService jwt;
    private final KeyValueStore store;
    private final String publicUrl;

    public OAuth2LoginSuccessHandler(UserRepository users, JwtService jwt, KeyValueStore store,
                                     @Value("${app.public-url:}") String publicUrl) {
        this.users = users;
        this.jwt = jwt;
        this.store = store;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();

        OAuth2User principal = (OAuth2User) auth.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        Object verifiedAttr = principal.getAttribute("email_verified");
        boolean verified = Boolean.TRUE.equals(verifiedAttr) || "true".equalsIgnoreCase(String.valueOf(verifiedAttr));

        if (email == null || email.isBlank() || !verified) {
            res.sendRedirect(publicUrl + "/oauth/callback#error=email_not_verified");
            return;
        }
        email = email.trim().toLowerCase();

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            res.sendRedirect(publicUrl + "/oauth/callback#token=" + jwt.generate(existing.get()));
            return;
        }

        String cleanName = name == null ? "" : name.replace('\t', ' ').trim();
        String ticket = UUID.randomUUID().toString();
        store.put(TICKET_PREFIX + ticket, email + "\t" + cleanName, TICKET_TTL);
        res.sendRedirect(publicUrl + "/oauth/callback#signup=" + ticket
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(cleanName, StandardCharsets.UTF_8));
    }
}
