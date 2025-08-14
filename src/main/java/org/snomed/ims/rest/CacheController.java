package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.User;
import org.snomed.ims.service.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.service.IdentityProvider;
import org.snomed.ims.service.CompressedTokenService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "CacheController")
public class CacheController {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheController.class);

	private final IdentityProvider identityProvider;
	private final CacheManager cacheManager;
	private final CompressedTokenService compressedTokenService;
	private final String cookieName;

	public CacheController(IdentityProvider identityProvider, CacheManager cacheManager, CompressedTokenService compressedTokenService, ApplicationProperties applicationProperties) {
		this.identityProvider = identityProvider;
		this.cacheManager = cacheManager;
		this.compressedTokenService = compressedTokenService;
		this.cookieName = applicationProperties.getCookieName();
	}

	/**
	 * Clear cache for all users
	 */
	@PostMapping("/cache/clear-all")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> clearCache(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null || cookies.length == 0) {
			return new ResponseEntity<>("Required cookie missing", HttpStatus.FORBIDDEN);
		}

		return doClearCache(response, cookies);
	}

	private ResponseEntity<String> doClearCache(HttpServletResponse response, Cookie[] cookies) {
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
				// Decompress the token from the cookie
				String compressedToken = cookie.getValue();
				String accessToken = compressedTokenService.decompressToken(compressedToken);
				
				if (accessToken == null) {
					LOGGER.error("Failed to decompress token from cookie");
					cookie.setMaxAge(0);
					cookie.setValue("");
					cookie.setPath("/");
					response.addCookie(cookie);
					return new ResponseEntity<>("Failed to decompress token", HttpStatus.FORBIDDEN);
				}
				
				User user = identityProvider.getUserByToken(accessToken);
				if (user == null) {
					LOGGER.error("4a19d36a-7cd1-4f25-be16-c7c19d63238e Failed to find user by token; invalidating cookie.");

					cookie.setMaxAge(0);
					cookie.setValue("");
					cookie.setPath("/");
					response.addCookie(cookie);

					return new ResponseEntity<>("User not found from token", HttpStatus.FORBIDDEN);
				}

				boolean hasPermission = user.getRoles().contains(AuthoritiesConstants.IMS_ADMIN);
				if (!hasPermission) {
					LOGGER.error("b357c992-8586-4a82-a994-9a504bf68bd9 Failed to clear cache; incorrect permissions.");
					return new ResponseEntity<>("User lacks permission", HttpStatus.FORBIDDEN);
				}

				cacheManager.getCacheNames().parallelStream().forEach(name -> {
					Cache cache = cacheManager.getCache(name);
					if (cache != null) {
						LOGGER.info("Cache {} cleared", name);
						cache.clear();
					}
				});
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
