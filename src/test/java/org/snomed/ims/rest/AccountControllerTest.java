
package org.snomed.ims.rest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.User;

import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class AccountControllerTest extends IntegrationTest {
	private static final String GET_ACCOUNT = "/account";
	private static final String POST_LOG_OUT = "/account/logout";

	@Test
	void getAccount_ShouldReturnExpected_WhenNoCookie() {
		// when
		ResultActions resultActions = get(GET_ACCOUNT);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(403, status);
		assertTrue(body.contains("\"authenticated\":false"));
		assertTrue(body.contains("\"loginUrl\""));
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenUnknownCookie() {
		// given
		Cookie cookie = new Cookie("unknown", "unknown");

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(403, status);
		assertTrue(body.contains("\"authenticated\":false"));
		assertTrue(body.contains("\"loginUrl\""));
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenFailingToGetUser() {
		// given
		// Use a simple test token (no compression needed)
		String testToken = "test-access-token";
		
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), testToken);
		givenGetUserByTokenThrowsException();

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(403, status);
		assertTrue(body.contains("\"authenticated\":false"));
		assertTrue(body.contains("\"loginUrl\""));
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenSuccessfullyGettingUser() {
		// given
		// Use a simple test token (no compression needed)
		String testToken = "test-access-token";
		
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), testToken);
		User user = new User();
		user.setLogin("test-login");

		givenGetUserByTokenReturnsExpected(user);

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);
		String responseHeader = getResponseHeader(resultActions, "X-AUTH-username");

		// then
		assertEquals(200, status);
		assertTrue(body.contains("\"authenticated\":true"));
		assertTrue(body.contains("\"test-login\""));
		assertEquals("test-login", responseHeader);
	}

	@Test
	void logout_ShouldReturnExpected_WhenGivenNoCookie() {
		// when
		ResultActions resultActions = post(POST_LOG_OUT);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(200, status);
		assertEquals("", body);
	}

	@Test
	void logout_ShouldReturnExpected_WhenGivenCookie() {
		// given
		// Use a simple test token (no compression needed)
		String testToken = "test-access-token";
		
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), testToken);

		// when
		ResultActions resultActions = post(POST_LOG_OUT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(200, status);
		assertEquals("", body);
	}

	private void givenGetUserByTokenThrowsException() {
		when(identityProvider.getUserByToken(anyString())).thenThrow(RuntimeException.class);
	}

	private void givenGetUserByTokenReturnsExpected(User user) {
		when(identityProvider.getUserByToken(anyString())).thenReturn(user);
	}
}
