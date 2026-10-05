package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

	Optional<Client> findByIdAndUserId(Long id, Long userId);

	List<Client> findByUserId(Long userId);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	boolean existsByPhone(String phone);

	boolean existsByPhoneAndIdNot(String phone, Long id);
}
