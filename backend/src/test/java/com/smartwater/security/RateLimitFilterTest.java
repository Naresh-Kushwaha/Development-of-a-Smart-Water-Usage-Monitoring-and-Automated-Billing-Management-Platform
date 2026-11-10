package com.smartwater.security;

import com.smartwater.store.KeyValueStore;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {
    private final RateLimitFilter filter = new RateLimitFilter(new KeyValueStore(null, false), 3);

    private MockHttpServletResponse call(String method, String path, String forwardedFor) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) req.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void blocksAfterTheLimitWith429() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(200, call("POST", "/api/auth/login", null).getStatus());

        MockHttpServletResponse blocked = call("POST", "/api/auth/login", null);

        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("Too many attempts"));
    }

    @Test
    void eachClientIpHasItsOwnBudget_usingTheForwardedHeader() throws Exception {
        for (int i = 0; i < 4; i++) call("POST", "/api/auth/login", "1.1.1.1, 10.0.0.9");

        assertEquals(429, call("POST", "/api/auth/login", "1.1.1.1").getStatus());
        assertEquals(200, call("POST", "/api/auth/login", "2.2.2.2").getStatus());
    }

    @Test
    void onlyPostsToTheAuthEndpointsAreLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, call("GET", "/api/auth/login", null).getStatus());
            assertEquals(200, call("POST", "/api/dashboard", null).getStatus());
        }
    }
}
