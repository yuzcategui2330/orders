package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT currentOrder FROM Order currentOrder WHERE currentOrder.id = :id")
	Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
