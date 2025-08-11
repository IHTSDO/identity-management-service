package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.service.IdentityProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@Tag(name = "AuthController")
public class AuthController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

	private final IdentityProvider identityProvider;
	private final String cookieName;
	private final Integer cookieMaxAge;
	private final String cookieDomain;
	private final boolean cookieSecureFlag;

	public AuthController(IdentityProvider identityProvider, ApplicationProperties applicationProperties) {
		this.identityProvider = identityProvider;
		this.cookieName = applicationProperties.getCookieName();
		this.cookieMaxAge = applicationProperties.getCookieMaxAgeInt();
		this.cookieDomain = applicationProperties.getCookieDomain();
		this.cookieSecureFlag = applicationProperties.isCookieSecure();
	}

	/**
	 * Check if the user is authenticated, and return their login.
	 */
	@GetMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
	public String isAuthenticated(HttpServletRequest request) {
		LOGGER.debug("REST request to check if the current user is authenticated");
		return request.getRemoteUser();
	}

	/**
	 * Validate user
	 */
	@PostMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Void> validateUser(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
		String username = authRequest.getLogin();
		String password = authRequest.getPassword();

		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		String token = identityProvider.authenticate(username, password);
		if (token == null) {
			LOGGER.error("Failed to authenticate");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Cookie cookie = new Cookie(cookieName, token);
		cookie.setMaxAge(cookieMaxAge);
		cookie.setDomain(cookieDomain);
		cookie.setSecure(cookieSecureFlag);
		cookie.setPath("/");
		response.addCookie(cookie);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * Initiate OAuth login flow - redirects to Keycloak
	 */
	@GetMapping("/auth/login")
	public ResponseEntity<Void> login(@RequestParam(value = "returnTo", defaultValue = "/") String returnTo, 
	                                  HttpServletRequest request) {
		LOGGER.debug("REST request to initiate OAuth login flow");
		
		String redirectUri = buildCallbackUrl(request);
		String authUrl = identityProvider.buildAuthorizationUrl(redirectUri, false);
		
		if (authUrl == null) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// Add returnTo to state parameter for callback
		String state = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
		authUrl += "&state=" + state;
		
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(authUrl))
				.build();
	}

	/**
	 * Handle OAuth callback from Keycloak
	 */
	@GetMapping("/auth/callback")
	public ResponseEntity<Void> callback(@RequestParam String code,
	                                    @RequestParam String state,
	                                    HttpServletRequest request,
	                                    HttpServletResponse response) {
		LOGGER.debug("REST request to handle OAuth callback");
		
		try {
			String redirectUri = buildCallbackUrl(request);
			String accessToken = identityProvider.exchangeCodeForAccessToken(code, redirectUri);
			
			if (accessToken == null || accessToken.isEmpty()) {
				LOGGER.error("Failed to exchange code for access token");
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
			
			// Set IMS session cookie
			Cookie imsCookie = new Cookie(cookieName, accessToken);
			if (cookieMaxAge != null) {
				imsCookie.setMaxAge(cookieMaxAge);
			}
			imsCookie.setDomain(cookieDomain);
			imsCookie.setSecure(cookieSecureFlag);
			imsCookie.setPath("/");
			imsCookie.setHttpOnly(true);
			imsCookie.setAttribute("SameSite", "Lax");
			response.addCookie(imsCookie);
			
			// Extract returnTo from state parameter
			String returnTo = "/";
			if (state != null && !state.isEmpty()) {
				returnTo = state;
			}
			
			// Redirect back to the SPA
			return ResponseEntity.status(HttpStatus.FOUND)
					.location(URI.create(returnTo))
					.build();
					
		} catch (Exception e) {
			LOGGER.error("Failed to handle OAuth callback", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Silent SSO auto-login (no page shown to user)
	 */
	@GetMapping("/auth/auto")
	public ResponseEntity<Void> autoLogin(@RequestParam(value = "returnTo", defaultValue = "/") String returnTo,
	                                     HttpServletRequest request) {
		LOGGER.debug("REST request to initiate silent SSO auto-login");
		
		String redirectUri = buildCallbackUrl(request);
		String authUrl = identityProvider.buildAuthorizationUrl(redirectUri, true);
		
		if (authUrl == null) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// Add returnTo to state parameter for callback
		String state = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
		authUrl += "&state=" + state;
		
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(authUrl))
				.build();
	}

	private String buildCallbackUrl(HttpServletRequest request) {
		// Build absolute URL to the callback endpoint
		String scheme = request.getHeader("X-Forwarded-Proto");
		if (scheme == null || scheme.isEmpty()) {
			scheme = request.getScheme();
		}
		String host = request.getHeader("X-Forwarded-Host");
		if (host == null || host.isEmpty()) {
			host = request.getHeader("Host");
		}
		// Fallbacks when Host headers are missing (e.g., during tests)
		if (host == null || host.isEmpty()) {
			host = request.getServerName();
			int port = request.getServerPort();
			boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
					|| ("https".equalsIgnoreCase(scheme) && port == 443);
			if (!isDefaultPort && port > 0) {
				host = host + ":" + port;
			}
		}
		
		// For tests, use a simple localhost URL
		if (host == null || host.isEmpty()) {
			return "http://localhost:8080/api/auth/callback";
		}
		
		String contextPath = request.getContextPath();
		if (contextPath == null) {
			contextPath = "";
		}
		
		return scheme + "://" + host + contextPath + "/api/auth/callback";
	}
}
