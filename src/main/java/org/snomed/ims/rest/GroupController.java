package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.User;
import org.snomed.ims.service.IdentityProvider;
import org.snomed.ims.service.CompressedTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.List;

@RestController
@Tag(name = "GroupController")
public class GroupController {
	private final IdentityProvider identityProvider;
	private final CompressedTokenService compressedTokenService;

	private final String cookieName;

	public GroupController(IdentityProvider identityProvider, CompressedTokenService compressedTokenService, ApplicationProperties applicationProperties) {
		this.identityProvider = identityProvider;
		this.compressedTokenService = compressedTokenService;
		this.cookieName = applicationProperties.getCookieName();
	}

	@GetMapping(value = "/group/user",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<User>> getUserByGroup(@RequestParam String groupname,
													 @RequestParam(required = false) String username,
													 @RequestParam int maxResults,
													 @RequestParam int startAt,
													 HttpServletRequest request,
													 HttpServletResponse response) {
		User currentUser = getCurrentUser(request, response);
		List<User> users = identityProvider.searchUsersByGroup(currentUser.getId(), groupname, username, maxResults, startAt);
		return new ResponseEntity<>(users, HttpStatus.OK);
	}

	private User getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
		User user = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					try {
						// Decompress the token from the cookie
						String compressedToken = cookie.getValue();
						String accessToken = compressedTokenService.decompressToken(compressedToken);
						
						if (accessToken == null) {
							// Failed to decompress token, invalidate cookie
							cookie.setMaxAge(0);
							cookie.setValue("");
							cookie.setPath("/");
							response.addCookie(cookie);
							return null;
						}
						
						user = identityProvider.getUserByToken(accessToken);
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
