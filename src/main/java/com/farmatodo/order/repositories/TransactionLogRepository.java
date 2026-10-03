package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
}
