package com.smartwater.repo;

import com.smartwater.domain.TariffPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<TariffPlan, Long> {
    Optional<TariffPlan> findByApartmentId(Long apartmentId);
}
