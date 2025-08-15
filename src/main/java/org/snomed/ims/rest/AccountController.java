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
import org.snomed.ims.domain.AuthenticationResponse;
import org.snomed.ims.domain.User;
import org.snomed.ims.service.IdentityProvider;
import org.snomed.ims.service.KeyCloakIdentityProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
						try {
							// Invalidate the opaque token with Keycloak
							String token = cookie.getValue();
							identityProvider.invalidateToken(token);
							LOGGER.debug("Successfully invalidated token with identity provider");
						} catch (Exception e) {
							LOGGER.error("Error during token invalidation, but continuing with cookie cleanup", e);
						}
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
    public ResponseEntity<AuthenticationResponse> getAccount(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (isCookieValid(cookie)) {
					try {
						// Get the opaque token from the cookie (no decompression needed)
						String token = cookie.getValue();
						
						// Introspect the token to get user information
						User user = null;
						if (identityProvider instanceof KeyCloakIdentityProvider) {
							user = ((KeyCloakIdentityProvider) identityProvider).introspectToken(token);
						} else {
							// Fallback to existing method for other identity providers
							user = identityProvider.getUserByToken(token);
						}
						if (user == null) {
							LOGGER.error("60037224-9b55-4f37-b944-eb4c1abc8fd9 Failed to get user; invalidating cookie");

							cookie.setMaxAge(0);
							cookie.setValue("");
							cookie.setPath("/");
							response.addCookie(cookie);

							// Return 403 with login URL instead of 302 redirect
							String loginUrl = buildLoginUrl(request);
							return ResponseEntity.status(HttpStatus.FORBIDDEN)
									.body(AuthenticationResponse.unauthenticated(loginUrl));
						}

						response.setHeader("Content-Type", "application/json;charset=UTF-8");
						response.setHeader(AUTH_HEADER_USERNAME, user.getLogin());
						response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(user.getRoles(), ","));

						return ResponseEntity.ok(AuthenticationResponse.authenticated(user));
					} catch (Exception e) {
						LOGGER.error("Failed to get user by token; invalidating cookie", e);
						
						cookie.setMaxAge(0);
						cookie.setValue("");
						cookie.setPath("/");
						response.addCookie(cookie);

						// Return 403 with login URL when exception occurs
						String loginUrl = buildLoginUrl(request);
						return ResponseEntity.status(HttpStatus.FORBIDDEN)
								.body(AuthenticationResponse.unauthenticated(loginUrl));
					}
				}
			}
		}

        // No IMS cookie. Handle OIDC passive check via prompt=none
        return getAccountForRequestWithoutCookie(request, response);
    }

    private boolean isCookieValid(Cookie cookie) {
        return cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0;
    }

    private ResponseEntity<AuthenticationResponse> getAccountForRequestWithoutCookie(HttpServletRequest request, HttpServletResponse response) {
        String error = request.getParameter("error");
        String code = request.getParameter("code");

        // If Keycloak indicated login is required, return 403 with login URL
        if ("login_required".equals(error)) {
            String loginUrl = buildLoginUrl(request);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AuthenticationResponse.unauthenticated(loginUrl));
        }

        // If Keycloak returned an auth code, exchange it for tokens and set IMS cookie
        if (code != null && !code.isEmpty()) {
            try {
                String redirectUri = buildRedirectUri(request);
                String accessToken = identityProvider.exchangeCodeForAccessToken(code, redirectUri);
                if (accessToken == null || accessToken.isEmpty()) {
                    String loginUrl = buildLoginUrl(request);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(AuthenticationResponse.unauthenticated(loginUrl));
                }

                // set IMS cookie with full token (this is a fallback path, main OAuth flow uses session IDs)
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
                    String loginUrl = buildLoginUrl(request);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(AuthenticationResponse.unauthenticated(loginUrl));
                }
                response.setHeader("Content-Type", "application/json;charset=UTF-8");
                response.setHeader(AUTH_HEADER_USERNAME, user.getLogin());
                response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(user.getRoles(), ","));
                return ResponseEntity.ok(AuthenticationResponse.authenticated(user));
            } catch (Exception e) {
                LOGGER.error("ea9b3b98-8f47-4d8c-9b93-9b7a23b6d2f3 Failed to exchange code for token", e);
                String loginUrl = buildLoginUrl(request);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(AuthenticationResponse.unauthenticated(loginUrl));
            }
        }

        // No valid session, return 403 with login URL instead of 302 redirect
        String loginUrl = buildLoginUrl(request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthenticationResponse.unauthenticated(loginUrl));
    }

    private String buildLoginUrl(HttpServletRequest request) {
        // Redirect to frontend home page instead of current API endpoint
        String returnTo = URLEncoder.encode("/#/home", StandardCharsets.UTF_8);
        // Don't add /api prefix if context path already includes it
        String authPath = request.getContextPath().endsWith("/api") ? "/auth/login" : "/api/auth/login";
        return request.getContextPath() + authPath + "?returnTo=" + returnTo;
    }

    private String buildCurrentUrl(HttpServletRequest request) {
        // Build absolute URL to current endpoint (without query params)
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
