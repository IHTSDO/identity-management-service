package org.snomed.ims.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig {
	private static final List<String> PERMIT_ALL = List.of(
			"version", // health check
			"authenticate", // log in
			"account", // allow passive /account to initiate OIDC check
			"account/logout" // log out
	);

	private final ApplicationProperties applicationProperties;

	public WebSecurityConfig(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// Disable for API usage
		http.csrf(AbstractHttpConfigurer::disable);

		if (applicationProperties.isBasicAuthEnabled()) {
			// Endpoints open
			for (String endpoint : PERMIT_ALL) {
				http.authorizeHttpRequests(auth -> auth.requestMatchers(new AntPathRequestMatcher(endpoint)).permitAll());
			}

			// Endpoints closed by basic
			http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

			// Configure exception handling to prevent Basic Auth popup
			// Returns JSON response instead of triggering browser Basic Auth popup
			http.exceptionHandling(exceptions -> exceptions
					.authenticationEntryPoint((request, response, authException) -> {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("application/json;charset=UTF-8");
						String message = authException.getMessage() != null ? authException.getMessage().replace("\"", "\\\"") : "Authentication required";
						response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
					})
			);
		}

		return http.build();
	}
}