package com.smartwater.repo;

import com.smartwater.domain.BulkPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface PurchaseRepository extends JpaRepository<BulkPurchase, Long> {
    List<BulkPurchase> findByApartmentIdOrderByPurchaseDateDesc(Long apartmentId);
    List<BulkPurchase> findByApartmentIdAndPurchaseDateBetween(Long apartmentId, LocalDate from, LocalDate to);
    Optional<BulkPurchase> findByIdAndApartmentId(Long id, Long apartmentId);
}
