package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	List<Payment> findByOrderIdIn(Collection<Long> orderIds);
}
