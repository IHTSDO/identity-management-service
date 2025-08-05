package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.service.IdentityProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "AccountController")
public class AccountController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
	private static final String AUTH_HEADER_PREFIX = "X-AUTH-";
	private static final String AUTH_HEADER_USERNAME = AUTH_HEADER_PREFIX + "username";

	private final String cookieName;
	private final IdentityProvider identityProvider;

	public AccountController(IdentityProvider identityProvider, ApplicationProperties applicationProperties) {
		this.identityProvider = identityProvider;
		this.cookieName = applicationProperties.getCookieName();
	}

	/**
	 * Logout
	 */
	@PostMapping("/account/logout")
	@ResponseStatus(HttpStatus.OK)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (request.isRequestedSessionIdValid() && session != null) {
			session.invalidate();
		}

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					if (StringUtils.isNotEmpty(cookie.getValue())) {
						identityProvider.invalidateToken(cookie.getValue());
					}

					// invalidate cookie
					cookie.setMaxAge(0);
					cookie.setValue("");
					cookie.setPath("/");
					response.addCookie(cookie);
				}
			}
		}
	}

	/**
	 * Get the current user
	 */
	@GetMapping(value = "/account", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> getAccount(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					User user = identityProvider.getUserByToken(cookie.getValue());
					if (user == null) {
						LOGGER.error("60037224-9b55-4f37-b944-eb4c1abc8fd9 Failed to get user; invalidating cookie");

						cookie.setMaxAge(0);
						cookie.setValue("");
						cookie.setPath("/");
						response.addCookie(cookie);

						return new ResponseEntity<>(HttpStatus.FORBIDDEN);
					}

					response.setHeader("Content-Type", "application/json;charset=UTF-8");
					response.setHeader(AUTH_HEADER_USERNAME, user.getLogin());
					response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(user.getRoles(), ","));

					return new ResponseEntity<>(user, HttpStatus.OK);
				}
			}
		}

		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}
}
