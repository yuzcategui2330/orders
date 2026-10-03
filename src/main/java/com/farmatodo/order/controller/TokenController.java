package com.farmatodo.order.controller;

import com.farmatodo.order.domains.response.CardTokenResponse;
import com.farmatodo.order.domains.request.TokenizeCardRequest;
import com.farmatodo.order.services.TokenizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

	private final TokenizationService tokenizationService;

	@PostMapping
	public ResponseEntity<CardTokenResponse> tokenize(@Valid @RequestBody TokenizeCardRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(tokenizationService.tokenize(request));
	}
}
