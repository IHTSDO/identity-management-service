package org.ihtsdo.otf.im.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig {
	private static final String[] ANONYMOUS = {
			"/api/reset_password", "/api/forgot_password"
	};

	private static final String[] PERMIT_ALL = {
			"/v3/api-docs/**", "/swagger-ui/**",
			"/api/authenticate", "/api/account", "/api/account/logout", "/api/cache/**",
			"/api/user", "/api/group/**"
	};

	private static final String[] AUTHENTICATED = {
			"/api/**"
	};

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(ANONYMOUS).anonymous()
						.requestMatchers(PERMIT_ALL).permitAll()
						.requestMatchers(AUTHENTICATED).authenticated()
				)
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}