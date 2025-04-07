package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserUpdateRequest;
import org.snomed.ims.middle.CrowdRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
	public ResponseEntity<User> updateUser(HttpServletRequest request, @RequestBody UserUpdateRequest requestBody) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					User user = crowdRestClient.getUserByToken(cookie.getValue());
					if (user == null) {
						return new ResponseEntity<>(HttpStatus.NOT_FOUND);
					} else {
						return new ResponseEntity<>(crowdRestClient.updateUser(user, requestBody), HttpStatus.OK);
					}
				}
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@GetMapping(value = "/user/role", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<String>> getUserRoles(@RequestParam String username) {
		return new ResponseEntity<>(crowdRestClient.getUserRoles(username), HttpStatus.OK);
	}

}
