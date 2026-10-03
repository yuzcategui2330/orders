package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

	Optional<CreditCard> findByTokenAndClientId(String token, Long clientId);
}
