package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			SELECT product
			FROM Product product
			WHERE (product.stockQuantity - product.reservedQty) > :minStock
			  AND (
			    LOWER(product.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
			    OR LOWER(product.shortName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
			    OR LOWER(product.description) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
			  )
			""")
	Page<Product> searchByNameOrShortName(
			@Param("query") String query,
			@Param("minStock") int minStock,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT product FROM Product product WHERE product.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
