package com.smartwater.repo;

import com.smartwater.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(Long invoiceId, Payment.Status status);
}
