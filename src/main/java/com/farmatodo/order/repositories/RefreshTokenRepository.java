package com.farmatodo.order.repositories;

import com.farmatodo.order.domains.RefreshToken;
import com.farmatodo.order.domains.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	@Query("SELECT token FROM RefreshToken token JOIN FETCH token.user WHERE token.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE RefreshToken token SET token.revoked = true WHERE token.user = :user AND token.revoked = false")
	void revokeByUser(@Param("user") User user);
}
