package com.farmatodo.order.config;

import com.farmatodo.order.services.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, TokenizationProperties.class})
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtService jwtService,
			TokenizationProperties tokenizationProperties) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh-token").permitAll()
						.requestMatchers(HttpMethod.POST, "/clients/make-registration").permitAll()
						.requestMatchers(HttpMethod.GET, "/products/search").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/tokens").authenticated()
						.requestMatchers(HttpMethod.POST, "/auth/change-password").authenticated()
						.requestMatchers("/clients", "/clients/**").authenticated()
						.requestMatchers("/orders", "/orders/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) ->
						JwtAuthenticationFilter.writeUnauthorized(response, JwtAuthenticationFilter.INVALID_ACCESS_TOKEN)))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), AuthorizationFilter.class)
				.addFilterBefore(new ApiKeyAuthenticationFilter(tokenizationProperties), AuthorizationFilter.class);
		return http.build();
	}
}
