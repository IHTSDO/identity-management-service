package org.ihtsdo.otf.im.web.filter;

import com.atlassian.crowd.integration.springsecurity.CrowdSSOAuthenticationProcessingFilter;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.ihtsdo.otf.im.service.UserDetailsService;
import org.ihtsdo.otf.im.web.rest.AccountResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.ihtsdo.otf.im.web.rest.AccountResource.AUTH_HEADER_USERNAME;

/**
 * Early filter to cache and return requests to GET /account without going through costly authentication filters.
 * Calls to /logout should remove the account from cache.
 */
public class CachedAccountFilter implements Filter {

	private static final String ACCOUNT_RESPONSE_CACHE = "account-response-cache";
	private final String cookieTokenKey;
	private Cache usernameCache;
	private final UserDetailsService userDetailsService;
	private final AccountResource accountResource;

	private static final Logger LOGGER = LoggerFactory.getLogger(CachedAccountFilter.class);

	public CachedAccountFilter(String cookieTokenKey, Cache usernameCache,
							   UserDetailsService userDetailsService, AccountResource accountResource) {
		this.cookieTokenKey = cookieTokenKey;
		Assert.notNull(usernameCache);
		this.usernameCache = usernameCache;
		this.userDetailsService = userDetailsService;
		this.accountResource = accountResource;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		/**
		 * TODO
		 *
		 * If GET /account
		 * 	Grab cookieTokenKey value and return cached response if we have one (cache needs to be limited and expire if logged out)
		 * 	else
		 * 		wrap servlet response to capture the X headers and body and cache on the way out.
		 *
		 * 	Cache insertion and lookup will be by tokenKey but logout may need to remove by username?
		 *
		 */

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String requestURI = getRequestURI(request);
		if ("/api/account".equals(requestURI) && request.getMethod().equals("GET")) {
			String authToken = getAuthToken(request);
			String cachedUsername = getUsername(authToken);
			if (cachedUsername != null) {
				SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(cachedUsername, null));
				LOGGER.debug("User from cache {}", cachedUsername);
				accountResource.getAccount((HttpServletResponse) servletResponse);
			} else {
				filterChain.doFilter(servletRequest, servletResponse);

				// Grab username for next time
				String username = ((HttpServletResponse) servletResponse).getHeader(AUTH_HEADER_USERNAME);
				if (username != null) {
					usernameCache.put(new Element(authToken, username));
				}
			}
		} else if (("/api/logout".equals(requestURI) || "/j_spring_security_logout".equals(requestURI)) && request.getMethod().equals("POST")) {
			String authToken = getAuthToken(request);
			String cachedUsername = getUsername(authToken);
			if (cachedUsername != null) {
				userDetailsService.logoutUsername(cachedUsername);
			}
			usernameCache.remove(authToken);
			LOGGER.info("Removed user from cache {}", cachedUsername);

			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private String getUsername(String authToken) {
		Element element = usernameCache.getQuiet(authToken);
		return element != null ? (String) element.getObjectValue() : null;
	}

	private String getAuthToken(HttpServletRequest request) {
		String authToken = null;
		for (Cookie cookie : request.getCookies()) {
			if (cookieTokenKey.equals(cookie.getName())) {
				authToken = cookie.getValue();
			}
		}
		return authToken;
	}

	private String getRequestURI(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null) {
			requestURI = requestURI.substring(contextPath.length());
		}
		return requestURI;
	}

	@Override
	public void destroy() {

	}
}
