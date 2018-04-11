package org.ihtsdo.otf.im.config;

import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationProcessingFilter;
import com.atlassian.crowd.service.soap.client.SoapClientProperties;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.ihtsdo.otf.im.security.AuthoritiesConstants;
import org.ihtsdo.otf.im.service.CacheManagerService;
import org.ihtsdo.otf.im.service.UserDetailsService;
import org.ihtsdo.otf.im.web.filter.CachedAccountFilter;
import org.ihtsdo.otf.im.web.filter.CsrfCookieGeneratorFilter;
import org.ihtsdo.otf.im.web.rest.AccountResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import javax.inject.Inject;

@Configuration
@EnableWebSecurity
@ImportResource({"classpath:applicationContext-CrowdClient.xml",
		"classpath:application-im-common-security-config.xml"})
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Inject
	private Environment env;

	@Inject
	private LoginUrlAuthenticationEntryPoint authenticationEntryPoint;

	@Inject
	private LogoutFilter logoutFilter;

	@Inject
	private CrowdSSOAuthenticationProcessingFilter ssoFilter;

	@Autowired
	public SoapClientProperties soapClientProperties;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService ourUserDetailsService() {
		Cache userDetailsCache = new Cache(new net.sf.ehcache.config.CacheConfiguration()
				.name("user-details-cache")
				.timeToLiveSeconds(60 * 60)
				.maxEntriesLocalHeap(1000)
				.maxBytesLocalDisk(124, MemoryUnit.MEGABYTES)
				.persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP)));
		CacheManager.create().addCache(userDetailsCache);
		cacheManagerService().addCache(userDetailsCache);
		return new UserDetailsService(userDetailsCache);
	}

	@Bean
	public AccountResource accountResource() {
		return new AccountResource();
	}

	@Bean
	public FilterRegistrationBean cachingSSOFilter() {
		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		Cache usernameCache = new Cache(new net.sf.ehcache.config.CacheConfiguration()
				.name("username-cache")
				.timeToLiveSeconds(60 * 60)
				.maxEntriesLocalHeap(1000)
				.maxBytesLocalDisk(124, MemoryUnit.MEGABYTES)
				.persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP)));
		CacheManager.create().addCache(usernameCache);
		cacheManagerService().addCache(usernameCache);
		registrationBean.setFilter(new CachedAccountFilter(soapClientProperties.getCookieTokenKey(), usernameCache,
				ourUserDetailsService(), accountResource()));
		registrationBean.setOrder(1);
		return registrationBean;
	}

	@Bean
	public CacheManagerService cacheManagerService() {
		return new CacheManagerService();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring()
				.antMatchers("/scripts/**/*.{js,html}")
				.antMatchers("/bower_components/**")
				.antMatchers("/i18n/**")
				.antMatchers("/assets/**")
				.antMatchers("/swagger-ui/**")
				.antMatchers("/test/**")
				.antMatchers("/console/**")
				.antMatchers("/dist/scripts/**/*.{js,html}")//production build
				.antMatchers("/dist/assets/**")//production build
				.antMatchers("/dist/bower_components/**")//production build
				.antMatchers("/dist/swagger-ui/**")//production build
				.antMatchers("/test/**")
				.antMatchers("/console/**")
				.antMatchers("/dist/**")//production build
				.and()
				.debug(env.getProperty("application.security.debug.flag", Boolean.class, false));
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.addFilterAfter(new CsrfCookieGeneratorFilter(), CsrfFilter.class)
				.addFilter(ssoFilter)
				.addFilter(logoutFilter)
				.exceptionHandling()
					.authenticationEntryPoint(authenticationEntryPoint)
					.and()
				.headers()
					.frameOptions()
					.disable()
				.authorizeRequests()
					.antMatchers("/api/register",
							"/api/reset_password",
							"/api/forgot_password",
							"/api/activate").anonymous()
					.antMatchers("/api/pre-register-check").permitAll()
					.antMatchers("/api/authenticate").permitAll()
					.antMatchers("/logout").permitAll()
					.antMatchers("/api/logs/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/api/**").authenticated()
					.antMatchers("/metrics/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/health/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/dump/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/shutdown/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/beans/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/configprops/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/info/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/autoconfig/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/env/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/trace/**").hasAuthority(AuthoritiesConstants.IHTDO_OPS_ADMIN)
					.antMatchers("/api-docs/**").permitAll()
					.antMatchers("/protected/**").authenticated()
					.antMatchers("/j_spring_security_logout").permitAll()
					.antMatchers("/j_security_check").permitAll();

	}

	@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
	private static class GlobalSecurityConfiguration extends GlobalMethodSecurityConfiguration {
	}

}
