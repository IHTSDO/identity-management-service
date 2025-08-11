package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.domain.User;
import org.snomed.ims.service.IdentityProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "AccountController")
public class AccountController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
    private static final String AUTH_HEADER_PREFIX = "X-AUTH-";
    private static final String AUTH_HEADER_USERNAME = AUTH_HEADER_PREFIX + "username";

    private final IdentityProvider identityProvider;

    private final String cookieName;
    private final Integer cookieMaxAge;
    private final String cookieDomain;
    private final boolean cookieSecureFlag;


    public AccountController(IdentityProvider identityProvider, ApplicationProperties applicationProperties) {
        this.identityProvider = identityProvider;
        this.cookieName = applicationProperties.getCookieName();
        this.cookieMaxAge = applicationProperties.getCookieMaxAgeInt();
        this.cookieDomain = applicationProperties.getCookieDomain();
        this.cookieSecureFlag = applicationProperties.isCookieSecure();
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
				if (isCookieValid(cookie)) {
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
				if (isCookieValid(cookie)) {
					User user = identityProvider.getUserByToken(cookie.getValue());
                    if (user == null) {
                        LOGGER.error("60037224-9b55-4f37-b944-eb4c1abc8fd9 Failed to get user; invalidating cookie and initiating passive OIDC check");

                        cookie.setMaxAge(0);
                        cookie.setValue("");
                        cookie.setPath("/");
                        response.addCookie(cookie);

                        String authUrl = identityProvider.buildAuthorizationUrl(buildRedirectUri(request), true);
                        return authUrl == null ? new ResponseEntity<>(HttpStatus.FORBIDDEN) : ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
                    }

					response.setHeader("Content-Type", "application/json;charset=UTF-8");
					response.setHeader(AUTH_HEADER_USERNAME, user.getLogin());
					response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(user.getRoles(), ","));

					return new ResponseEntity<>(user, HttpStatus.OK);
				}
			}
		}

        // No IMS cookie. Handle OIDC passive check via prompt=none
        return getAccountForRequestWithoutCookie(request, response);
    }

    private boolean isCookieValid(Cookie cookie) {
        return cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0;
    }

    private ResponseEntity<User> getAccountForRequestWithoutCookie(HttpServletRequest request, HttpServletResponse response) {
        String error = request.getParameter("error");
        String code = request.getParameter("code");

        // If Keycloak indicated login is required, redirect to interactive login (no prompt=none)
        if ("login_required".equals(error)) {
            String interactiveAuthUrl = identityProvider.buildAuthorizationUrl(buildRedirectUri(request), false);
            return interactiveAuthUrl == null ? new ResponseEntity<>(HttpStatus.FORBIDDEN) : ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, interactiveAuthUrl)
                    .build();
        }

        // If Keycloak returned an auth code, exchange it for tokens and set IMS cookie
        if (code != null && !code.isEmpty()) {
            try {
                String redirectUri = buildRedirectUri(request);
                String accessToken = identityProvider.exchangeCodeForAccessToken(code, redirectUri);
                if (accessToken == null || accessToken.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }

                // set IMS cookie
                Cookie imsCookie = new Cookie(cookieName, accessToken);
                if (cookieMaxAge != null) {
                    imsCookie.setMaxAge(cookieMaxAge);
                }
                imsCookie.setDomain(cookieDomain);
                imsCookie.setSecure(cookieSecureFlag);
                imsCookie.setPath("/");
                response.addCookie(imsCookie);

                User user = identityProvider.getUserByToken(accessToken);
                if (user == null) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
                response.setHeader("Content-Type", "application/json;charset=UTF-8");
                response.setHeader(AUTH_HEADER_USERNAME, user.getLogin());
                response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(user.getRoles(), ","));
                return new ResponseEntity<>(user, HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("ea9b3b98-8f47-4d8c-9b93-9b7a23b6d2f3 Failed to exchange code for token", e);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }

        // Initiate passive check with prompt=none to avoid login prompt
        String authUrl = identityProvider.buildAuthorizationUrl(buildRedirectUri(request), true);
        return authUrl == null ? new ResponseEntity<>(HttpStatus.FORBIDDEN) : ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    private String buildRedirectUri(HttpServletRequest request) {
        // Build absolute URL to this endpoint (without query params)
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
        String requestUri = request.getRequestURI();
        return scheme + "://" + host + requestUri;
    }

}
