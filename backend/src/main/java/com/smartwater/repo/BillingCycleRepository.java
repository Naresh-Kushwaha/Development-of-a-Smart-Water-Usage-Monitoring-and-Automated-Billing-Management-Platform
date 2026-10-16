package com.smartwater.repo;

import com.smartwater.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {
    List<BillingCycle> findByApartmentIdOrderByStartDateDesc(Long apartmentId);
    Optional<BillingCycle> findByIdAndApartmentId(Long id, Long apartmentId);
    Optional<BillingCycle> findFirstByApartmentIdAndStatusOrderByStartDateDesc(Long apartmentId, Enums.CycleStatus status);
}
