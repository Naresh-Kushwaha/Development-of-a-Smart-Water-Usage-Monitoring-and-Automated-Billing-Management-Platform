package com.smartwater.repo;

import com.smartwater.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    List<Household> findByApartmentIdOrderByUnitNumber(Long apartmentId);
    Optional<Household> findByApartmentIdAndUnitNumberIgnoreCase(Long apartmentId, String unitNumber);
    Optional<Household> findByIdAndApartmentId(Long id, Long apartmentId);
}
