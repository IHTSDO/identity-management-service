package org.snomed.ims.rest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.crowd.Session;
import org.snomed.ims.domain.User;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClientException;

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
        String location = getResponseHeader(resultActions, "Location");

		// then
        assertEquals(302, status);
        assertTrue(location != null && location.contains("/protocol/openid-connect/auth") && location.contains("prompt=none"));
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenUnknownCookie() {
		// given
		Cookie cookie = new Cookie("unknown", "unknown");

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);
        String location = getResponseHeader(resultActions, "Location");

		// then
        assertEquals(302, status);
        assertTrue(location != null && location.contains("/protocol/openid-connect/auth") && location.contains("prompt=none"));
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenFailingToGetUser() {
		// given
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), "value");
		givenGetUserByTokenThrowsException();

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);

		// then
		assertEquals(403, status);
	}

	@Test
	void getAccount_ShouldReturnExpected_WhenSuccessfullyGettingUser() {
		// given
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), "value");
		User user = new User();
		user.setLogin("test-login");

		Session session = new Session();
		session.setUser(user);
		givenGetUserByTokenReturnsExpected(session);

		// when
		ResultActions resultActions = get(GET_ACCOUNT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);
		String responseHeader = getResponseHeader(resultActions, "X-AUTH-username");

		// then
		assertEquals(200, status);
		assertEquals("{\"login\":\"test-login\",\"active\":false,\"username\":\"test-login\"}", body);
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
		Cookie cookie = new Cookie(applicationProperties.getCookieName(), "value");

		// when
		ResultActions resultActions = post(POST_LOG_OUT, cookie);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(200, status);
		assertEquals("", body);
	}

	private void givenGetUserByTokenThrowsException() {
		when(restTemplate.getForObject(anyString(), eq(Session.class), anyMap())).thenThrow(RestClientException.class);
	}

	private void givenGetUserByTokenReturnsExpected(Session session) {
		when(restTemplate.getForObject(anyString(), eq(Session.class), anyMap())).thenReturn(session);
	}
}