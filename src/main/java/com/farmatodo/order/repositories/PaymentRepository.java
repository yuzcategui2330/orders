package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
