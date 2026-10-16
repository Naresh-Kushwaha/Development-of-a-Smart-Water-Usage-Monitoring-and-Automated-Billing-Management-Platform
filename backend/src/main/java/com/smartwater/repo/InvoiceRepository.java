package com.smartwater.repo;

import com.smartwater.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByBillingCycleIdOrderByHouseholdUnitNumber(Long cycleId);
    List<Invoice> findByHouseholdIdOrderByCreatedAtDesc(Long householdId);
}
