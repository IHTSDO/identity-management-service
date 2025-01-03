package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.middle.CrowdRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "AuthController")
public class AuthController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

	private final CrowdRestClient crowdRestClient;
	private final String cookieName;
	private final Integer cookieMaxAge;
	private final String cookieDomain;
	private final boolean cookieSecureFlag;

	public AuthController(CrowdRestClient crowdRestClient, ApplicationProperties applicationProperties) {
		this.crowdRestClient = crowdRestClient;
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
	public ResponseEntity<Void> validateUser(@RequestBody Map<String, String> authRequest, HttpServletResponse response) {
		String username = authRequest.get("login"); // note: existing clients use login
		String password = authRequest.get("password");

		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		String token = crowdRestClient.authenticate(username, password);
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
}
