package com.smartwater.web;

import com.smartwater.security.Current;
import com.smartwater.service.AuthService;
import com.smartwater.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register-admin")
    public AuthRes registerAdmin(@Valid @RequestBody RegisterAdminReq r) { return auth.registerAdmin(r); }

    @PostMapping("/register")
    public AuthRes register(@Valid @RequestBody RegisterResidentReq r) { return auth.registerResident(r); }

    @PostMapping("/login")
    public AuthRes login(@Valid @RequestBody LoginReq r) { return auth.login(r); }

    @GetMapping("/me")
    public UserRes me() { return Mapper.user(Current.user()); }

    @PutMapping("/me")
    public UserRes update(@Valid @RequestBody ProfileReq r) { return auth.updateProfile(Current.user(), r); }
}
