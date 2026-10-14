package com.smartwater.repo;

import com.smartwater.domain.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    Optional<Apartment> findByCode(String code);
}
