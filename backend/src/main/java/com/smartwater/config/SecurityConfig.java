package com.smartwater.config;

import com.smartwater.security.JwtAuthFilter;
import com.smartwater.security.OAuth2LoginSuccessHandler;
import com.smartwater.security.RateLimitFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter, RateLimitFilter rateLimitFilter,
                                    ObjectProvider<ClientRegistrationRepository> clients,
                                    OAuth2LoginSuccessHandler oauthSuccess,
                                    @Value("${app.public-url:}") String publicUrl) throws Exception {
        boolean googleEnabled = clients.getIfAvailable() != null;
        String base = publicUrl.replaceAll("/+$", "");

        http.csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                // The API itself is stateless (JWT). Only the short Google redirect dance needs a session for its "state" value.
                .sessionManagement(s -> s.sessionCreationPolicy(googleEnabled ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/register-admin",
                                "/api/auth/oauth/complete", "/api/auth/providers",
                                "/api/oauth2/**", "/api/login/oauth2/**",
                                "/api/payments/webhook",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Unauthorized")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

        if (googleEnabled) {
            // Endpoints live under /api so the existing proxies (Vite dev server, nginx, ALB rules) already forward them.
            http.oauth2Login(o -> o
                    .authorizationEndpoint(a -> a.baseUri("/api/oauth2/authorization"))
                    .redirectionEndpoint(r -> r.baseUri("/api/login/oauth2/code/*"))
                    .successHandler(oauthSuccess)
                    .failureHandler((req, res, ex) -> res.sendRedirect(base + "/oauth/callback#error=oauth_failed")));
        }
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") List<String> origins) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
