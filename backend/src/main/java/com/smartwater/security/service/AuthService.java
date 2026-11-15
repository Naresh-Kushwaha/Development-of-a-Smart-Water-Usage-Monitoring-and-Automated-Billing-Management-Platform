package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.*;
import com.smartwater.repo.*;
import com.smartwater.security.JwtService;
import com.smartwater.store.KeyValueStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import com.smartwater.web.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final ApartmentRepository apartments;
    private final HouseholdRepository households;
    private final TariffRepository tariffs;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final KeyValueStore store;

    @Transactional
    public AuthRes registerAdmin(RegisterAdminReq r) {
        ensureEmailFree(r.email());
        Apartment a = new Apartment();
        a.setName(r.apartmentName().trim());
        a.setAddress(r.address());
        a.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        a = apartments.save(a);

        TariffPlan t = new TariffPlan();           // sensible defaults, editable in the admin panel
        t.setApartment(a);
        t.setTier1Rate(new BigDecimal("12.00"));
        t.setTier2Rate(new BigDecimal("20.00"));
        tariffs.save(t);

        User u = newUser(r.email(), r.password(), r.fullName(), Role.ADMIN, a);
        return new AuthRes(jwt.generate(u), Mapper.user(u));
    }

    @Transactional
    public AuthRes registerResident(RegisterResidentReq r) {
        ensureEmailFree(r.email());
        Apartment a = apartments.findByCode(r.apartmentCode().trim().toUpperCase())
                .orElseThrow(() -> ApiException.bad("Unknown apartment code"));
        Household h = households.findByApartmentIdAndUnitNumberIgnoreCase(a.getId(), r.unitNumber().trim())
                .orElseThrow(() -> ApiException.bad("Unit not found - ask your apartment admin to register it first"));
        User u = newUser(r.email(), r.password(), r.fullName(), Role.RESIDENT, a);
        u.setHousehold(h);
        users.save(u);
        return new AuthRes(jwt.generate(u), Mapper.user(u));
    }

    public AuthRes login(LoginReq r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthRes(jwt.generate(u), Mapper.user(u));
    }

    @Transactional
    public UserRes updateProfile(User current, ProfileReq r) {
        User u = users.findById(current.getId()).orElseThrow();
        u.setFullName(r.fullName().trim());
        if (r.newPassword() != null && !r.newPassword().isBlank()) {
            if (r.newPassword().length() < 8) throw ApiException.bad("Password must be at least 8 characters");
            u.setPasswordHash(encoder.encode(r.newPassword()));
        }
        return Mapper.user(users.save(u));
    }

    /** Logout: remember the token's id until it would have expired anyway, so it can no longer be used. */
    public void logout(String token) {
        try {
            Claims c = jwt.parse(token);
            long ttlMillis = c.getExpiration().getTime() - System.currentTimeMillis();
            if (c.getId() != null && ttlMillis > 0)
                store.put("jwt:revoked:" + c.getId(), "1", Duration.ofMillis(ttlMillis));
        } catch (JwtException | IllegalArgumentException ignored) {
            // already invalid - nothing to revoke
        }
    }

    /**
     * Second step of "Sign up with Google": the browser comes back with the ticket we issued after Google
     * verified the email, plus the apartment code and unit the person lives in.
     */
    @Transactional
    public AuthRes completeOAuthSignup(OAuthCompleteReq r) {
        String key = "oauth:signup:" + r.ticket();
        String payload = store.get(key)
                .orElseThrow(() -> ApiException.bad("This Google sign-up has expired. Please sign in with Google again."));
        String[] parts = payload.split("\t", 2);
        String email = parts[0];
        String name = parts.length > 1 && !parts[1].isBlank() ? parts[1] : email.substring(0, email.indexOf('@'));

        // A random password nobody knows: this account signs in with Google (or a password reset later).
        AuthRes res = registerResident(new RegisterResidentReq(email, UUID.randomUUID().toString(), name,
                r.apartmentCode(), r.unitNumber()));
        store.delete(key);                       // single use - only after the account was really created
        return res;
    }

    private void ensureEmailFree(String email) {
        if (users.existsByEmail(email.trim().toLowerCase())) throw ApiException.conflict("Email already registered");
    }

    private User newUser(String email, String pw, String name, Role role, Apartment a) {
        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(pw));
        u.setFullName(name.trim());
        u.setRole(role);
        u.setApartment(a);
        u.setCreatedAt(Instant.now());
        return users.save(u);
    }
}
