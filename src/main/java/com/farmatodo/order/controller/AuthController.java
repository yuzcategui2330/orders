package com.farmatodo.order.controller;

import com.farmatodo.order.domains.request.ChangePasswordRequest;
import com.farmatodo.order.domains.request.LoginRequest;
import com.farmatodo.order.domains.request.RefreshTokenRequest;
import com.farmatodo.order.domains.response.TokenResponse;
import com.farmatodo.order.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
		authService.changePassword(request);
		return ResponseEntity.noContent().build();
	}
}
