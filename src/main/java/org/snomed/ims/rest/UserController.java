package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.User;
import org.snomed.ims.domain.UserPasswordUpdateRequest;
import org.snomed.ims.domain.UserInformationUpdateRequest;
import org.snomed.ims.service.IdentityProvider;
import org.snomed.ims.service.KeyCloakIdentityProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.List;

@RestController
@Tag(name = "UserController")
public class UserController {

	private final IdentityProvider identityProvider;


	private final String cookieName;

	public UserController(IdentityProvider identityProvider, ApplicationProperties applicationProperties) {
		this.identityProvider = identityProvider;
		this.cookieName = applicationProperties.getCookieName();
	}

	@GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> getUserDetails(@RequestParam String username) {
		User user = identityProvider.getUser(username);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(user, HttpStatus.OK);
		}
	}

	/**
	 * Update the current user information
	 */
	@PutMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> updateUser(HttpServletRequest request, HttpServletResponse response, @RequestBody UserInformationUpdateRequest requestBody) {
		User user;
		try {
			user = getCurrentUser(request, response);
		} catch (RestClientException ex) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			String token = null;
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
						// Get the opaque token from the cookie (no decompression needed)
						token = cookie.getValue();
						break;
					}
				}
			}
			return new ResponseEntity<>(identityProvider.updateUser(user, requestBody, token), HttpStatus.OK);
		}
	}

	@PutMapping(value = "/user/password",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> updateUserPassword(@RequestBody UserPasswordUpdateRequest requestBody, HttpServletRequest request, HttpServletResponse response) {
		User user;
		try {
			user = getCurrentUser(request, response);
		} catch (RestClientException ex) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			identityProvider.resetUserPassword(user, requestBody.newPassword());
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}

	@GetMapping(value = "/user/role", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<String>> getUserRoles(@RequestParam String username) {
		return new ResponseEntity<>(identityProvider.getUserRoles(username), HttpStatus.OK);
	}

	private User getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
		User user = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					try {
						// Get the opaque token from the cookie (no decompression needed)
						String token = cookie.getValue();
						
						// Introspect the token to get user information
						if (identityProvider instanceof KeyCloakIdentityProvider) {
							user = ((KeyCloakIdentityProvider) identityProvider).introspectToken(token);
						} else {
							// Fallback to existing method for other identity providers
							user = identityProvider.getUserByToken(token);
						}
					} catch (RestClientException ex) {
						// invalidate cookie
						cookie.setMaxAge(0);
						cookie.setValue("");
						cookie.setPath("/");
						response.addCookie(cookie);
						throw ex;
					}
				}
			}
		}
		return user;
	}

}
