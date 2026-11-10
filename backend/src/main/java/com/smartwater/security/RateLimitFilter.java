package com.smartwater.security;

import com.smartwater.store.KeyValueStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Slows down password guessing and sign-up spam: at most N POSTs per minute per client IP on the public
 * auth endpoints. Counters live in Redis, so the limit holds across all backend instances.
 *
 * The client IP is the first X-Forwarded-For entry when present (the bundled nginx sets it). That header
 * can be forged by a client that reaches the backend directly, so keep the backend behind your proxy/ALB.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/register-admin", "/api/auth/oauth/complete");

    private final KeyValueStore store;
    private final int limitPerMinute;

    public RateLimitFilter(KeyValueStore store, @Value("${app.rate-limit.auth-per-minute:20}") int limitPerMinute) {
        this.store = store;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long attempts = store.increment("rl:auth:" + clientIp(req), Duration.ofMinutes(1));
        if (attempts > limitPerMinute) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Too many attempts. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
