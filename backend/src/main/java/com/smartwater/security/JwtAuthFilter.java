package com.smartwater.security;

import com.smartwater.repo.UserRepository;
import com.smartwater.store.KeyValueStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String REVOKED_PREFIX = "jwt:revoked:";

    private final JwtService jwt;
    private final UserRepository users;
    private final KeyValueStore store;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                boolean revoked = claims.getId() != null && store.exists(REVOKED_PREFIX + claims.getId());
                if (!revoked) {
                    users.findByEmail(claims.getSubject()).ifPresent(u -> {
                        var auth = new UsernamePasswordAuthenticationToken(u, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid/expired token -> request stays unauthenticated -> 401
            }
        }
        chain.doFilter(req, res);
    }
}
