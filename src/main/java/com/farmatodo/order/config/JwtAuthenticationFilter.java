package com.farmatodo.order.config;

import com.farmatodo.order.services.AccessToken;
import com.farmatodo.order.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String INVALID_ACCESS_TOKEN = "Invalid access token";
	public static final String ACCESS_TOKEN_EXPIRED = "Access token expired";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(7).trim();
		if (token.isEmpty()) {
			writeUnauthorized(response, INVALID_ACCESS_TOKEN);
			return;
		}

		try {
			AccessToken accessToken = jwtService.parseAccessToken(token);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					accessToken,
					null,
					List.of());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException exception) {
			SecurityContextHolder.clearContext();
			writeUnauthorized(response, ACCESS_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException exception) {
			SecurityContextHolder.clearContext();
			writeUnauthorized(response, INVALID_ACCESS_TOKEN);
		}
	}

	static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"message\":\"" + message + "\"}");
	}
}
