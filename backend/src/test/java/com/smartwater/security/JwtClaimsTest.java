package com.smartwater.security;

import com.smartwater.domain.Enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static com.smartwater.TestData.apartment;
import static com.smartwater.TestData.user;
import static org.junit.jupiter.api.Assertions.*;

class JwtClaimsTest {
    private final JwtService jwt = new JwtService("0123456789abcdef0123456789abcdef", 60);
    private final com.smartwater.domain.User alice = user(1L, "alice@x.com", Role.RESIDENT, apartment(1L), null);

    @Test
    void everyTokenGetsItsOwnId_soOneTokenCanBeRevoked() {
        String id1 = jwt.parse(jwt.generate(alice)).getId();
        String id2 = jwt.parse(jwt.generate(alice)).getId();

        assertNotNull(id1);
        assertNotEquals(id1, id2);
    }

    @Test
    void parseExposesSubjectRoleAndExpiry() {
        Claims c = jwt.parse(jwt.generate(alice));

        assertEquals("alice@x.com", c.getSubject());
        assertEquals("RESIDENT", c.get("role", String.class));
        assertTrue(c.getExpiration().after(new Date()));
    }
}
