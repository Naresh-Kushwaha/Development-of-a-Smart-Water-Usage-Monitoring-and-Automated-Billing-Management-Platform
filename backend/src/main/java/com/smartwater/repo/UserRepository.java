package com.smartwater.repo;

import com.smartwater.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByHouseholdId(Long householdId);
    List<User> findByApartmentIdAndRole(Long apartmentId, Enums.Role role);
}
