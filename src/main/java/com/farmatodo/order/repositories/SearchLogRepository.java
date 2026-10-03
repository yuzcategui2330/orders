package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
