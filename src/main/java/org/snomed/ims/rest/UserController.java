package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserPasswordUpdateRequest;
import org.snomed.ims.domain.crowd.UserInformationUpdateRequest;
import org.snomed.ims.middle.CrowdRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.List;

@RestController
@Tag(name = "UserController")
public class UserController {

	private final CrowdRestClient crowdRestClient;

	private final String cookieName;

	public UserController(CrowdRestClient crowdRestClient, ApplicationProperties applicationProperties) {
		this.crowdRestClient = crowdRestClient;
		this.cookieName = applicationProperties.getCookieName();
	}

	@GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> getUserDetails(@RequestParam String username) {
		User user = crowdRestClient.getUser(username);
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
			return new ResponseEntity<>(crowdRestClient.updateUser(user, requestBody), HttpStatus.OK);
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
			crowdRestClient.resetUserPassword(user.getLogin(), requestBody.newPassword());
			return new ResponseEntity<>(HttpStatus.OK);
		}
	}

	@GetMapping(value = "/user/role", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<String>> getUserRoles(@RequestParam String username) {
		return new ResponseEntity<>(crowdRestClient.getUserRoles(username), HttpStatus.OK);
	}

	private User getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
		User user = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					try {
						user = crowdRestClient.getUserByToken(cookie.getValue());
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
