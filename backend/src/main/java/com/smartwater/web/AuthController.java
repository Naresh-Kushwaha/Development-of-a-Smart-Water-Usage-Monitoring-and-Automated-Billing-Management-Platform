package com.smartwater.web;

import com.smartwater.security.Current;
import com.smartwater.service.AuthService;
import com.smartwater.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final ObjectProvider<ClientRegistrationRepository> oauthClients;

    @PostMapping("/register-admin")
    public AuthRes registerAdmin(@Valid @RequestBody RegisterAdminReq r) { return auth.registerAdmin(r); }

    @PostMapping("/register")
    public AuthRes register(@Valid @RequestBody RegisterResidentReq r) { return auth.registerResident(r); }

    @PostMapping("/login")
    public AuthRes login(@Valid @RequestBody LoginReq r) { return auth.login(r); }

    /** Revokes the token that was used for this call (kept in Redis until it would have expired). */
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) auth.logout(header.substring(7));
    }

    /** Second step of "Sign up with Google". */
    @PostMapping("/oauth/complete")
    public AuthRes completeOAuth(@Valid @RequestBody OAuthCompleteReq r) { return auth.completeOAuthSignup(r); }

    /** Lets the login page show the Google button only when Google login is configured. */
    @GetMapping("/providers")
    public Map<String, Boolean> providers() { return Map.of("google", oauthClients.getIfAvailable() != null); }

    @GetMapping("/me")
    public UserRes me() { return Mapper.user(Current.user()); }

    @PutMapping("/me")
    public UserRes update(@Valid @RequestBody ProfileReq r) { return auth.updateProfile(Current.user(), r); }
}
