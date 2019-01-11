package org.ihtsdo.otf.im.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
			.antMatchers("/api/register",
					"/api/reset_password",
					"/api/forgot_password",
					"/api/activate").anonymous()
			.antMatchers("/api/pre-register-check").permitAll()
			.antMatchers("/api/authenticate").permitAll()
			.antMatchers("/api/account").permitAll()
			.antMatchers("/logout").permitAll()
			.antMatchers("/api/**").authenticated()
			.antMatchers("/j_spring_security_logout").permitAll()
			.antMatchers("/j_security_check").permitAll();
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth
				.inMemoryAuthentication()
				.withUser("user").password("password").roles("USER");
	}
}
