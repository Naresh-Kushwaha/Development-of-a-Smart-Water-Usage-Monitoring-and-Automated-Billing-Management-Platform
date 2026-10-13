package com.smartwater.security;

import com.smartwater.domain.User;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Current {
    private Current() {}
    public static User user() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    public static Long apartmentId() { return user().getApartment().getId(); }
}
