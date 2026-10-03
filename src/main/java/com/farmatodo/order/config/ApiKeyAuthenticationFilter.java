package com.farmatodo.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";
	public static final String INVALID_API_KEY = "Invalid API key";

	private final TokenizationProperties tokenizationProperties;

	public ApiKeyAuthenticationFilter(TokenizationProperties tokenizationProperties) {
		this.tokenizationProperties = tokenizationProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !isTokenizeRequest(request);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (!matches(request.getHeader(HEADER), tokenizationProperties.apiKey())) {
			SecurityContextHolder.clearContext();
			JwtAuthenticationFilter.writeUnauthorized(response, INVALID_API_KEY);
			return;
		}
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("tokenization", null, List.of()));
		filterChain.doFilter(request, response);
	}

	private boolean isTokenizeRequest(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return false;
		}
		String path = request.getServletPath();
		if (path == null || path.isBlank()) {
			path = request.getRequestURI();
		}
		return "/api/v1/tokens".equals(path);
	}

	private boolean matches(String provided, String expected) {
		if (provided == null || expected == null || expected.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
	}
}
