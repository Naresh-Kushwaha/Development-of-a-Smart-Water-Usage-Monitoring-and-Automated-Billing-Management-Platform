package com.smartwater;

import com.smartwater.domain.*;
import com.smartwater.repo.*;
import com.smartwater.security.JwtService;
import com.smartwater.service.AuthService;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    UserRepository users;
    @Mock
    ApartmentRepository apartments;
    @Mock
    HouseholdRepository households;
    @Mock
    TariffRepository tariffs;
    @Mock
    PasswordEncoder encoder;
    @Mock
    JwtService jwt;
    @InjectMocks
    AuthService service;

    @Test
    void residentRegistrationFailsForUnknownApartmentCode() {
        when(apartments.findByCode("NOPE")).thenReturn(Optional.empty());
        var ex = assertThrows(ApiException.class,
                () -> service.registerResident(new RegisterResidentReq("r@x.com", "Password1", "R", "nope", "A-1")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(users, never()).save(any());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmail("a@x.com")).thenReturn(true);
        var ex = assertThrows(ApiException.class,
                () -> service.registerAdmin(new RegisterAdminReq("a@x.com", "Password1", "A", "Apts", null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void wrongPasswordGives401() {
        User u = new User(); u.setPasswordHash("hash");
        when(users.findByEmail("a@x.com")).thenReturn(Optional.of(u));
        when(encoder.matches("bad", "hash")).thenReturn(false);
        var ex = assertThrows(ApiException.class, () -> service.login(new LoginReq("a@x.com", "bad")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
