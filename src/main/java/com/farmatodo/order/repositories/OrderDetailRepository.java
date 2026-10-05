package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

	List<OrderDetail> findByOrderId(Long orderId);

	List<OrderDetail> findByOrderIdIn(Collection<Long> orderIds);

	Optional<OrderDetail> findByOrderIdAndProductId(Long orderId, Long productId);
}
