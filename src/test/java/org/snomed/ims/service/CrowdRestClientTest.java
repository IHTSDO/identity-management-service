package org.snomed.ims.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.snomed.ims.domain.crowd.Group;
import org.snomed.ims.domain.crowd.GroupsCollection;
import org.snomed.ims.domain.crowd.Session;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UsersCollection;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrowdRestClientTest {
	private static final String EMPTY = "";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";

	private final RestTemplate restTemplate = mock(RestTemplate.class);
	private final CrowdRestClient crowdRestClient = new CrowdRestClient(restTemplate);

	@Test
	void authenticate_ShouldReturnExpected_WhenGivenEmptyUsername() {
		// when
		String token = crowdRestClient.authenticate(EMPTY, PASSWORD);

		// then
		assertNull(token);
	}

	@Test
	void authenticate_ShouldReturnExpected_WhenGivenEmptyPassword() {
		// when
		String token = crowdRestClient.authenticate(USERNAME, EMPTY);

		// then
		assertNull(token);
	}

	@Test
	void authenticate_ShouldReturnExpected_WhenResponseUnexpectedFormat() {
		// given
		givenUnexpectedResponseFormat();

		// when
		String token = crowdRestClient.authenticate(USERNAME, PASSWORD);

		// then
		assertNull(token);
	}

	@Test
	void authenticate_ShouldReturnExpected_WhenResponseMissingToken() {
		// given
		givenResponseWithoutToken();

		// when
		String token = crowdRestClient.authenticate(USERNAME, PASSWORD);

		// then
		assertNull(token);
	}

	@Test
	void authenticate_ShouldReturnExpected_WhenAuthCorrect() {
		// given
		Session session = new Session();
		session.setToken("iamthetoken");

		givenAuthCorrect(session);

		// when
		String token = crowdRestClient.authenticate(USERNAME, PASSWORD);

		// then
		assertEquals("iamthetoken", token);
	}

	@Test
	void authenticate_ShouldReturnExpected_WhenAuthIncorrect() {
		// given
		givenAuthIncorrect();

		// when
		String token = crowdRestClient.authenticate(USERNAME, PASSWORD);

		// then
		assertNull(token);
	}

	@Test
	void getUser_ShouldReturnExpected_WhenGivenNull() {
		// when
		User user = crowdRestClient.getUser(null);

		// then
		assertNull(user);
	}

	@Test
	void getUser_ShouldReturnExpected_WhenUserNotFound() {
		// given
		givenUserUnknown();

		// when
		User user = crowdRestClient.getUser("unknown");

		// then
		assertNull(user);
	}

	@Test
	void getUser_ShouldReturnExpected_WhenUserFound() {
		// given
		User expected = new User();
		expected.setFirstName("known");
		givenUserKnown(expected);

		// when
		User actual = crowdRestClient.getUser("known");

		// then
		assertEquals(expected, actual);
	}

	@Test
	void getUserByToken_ShouldReturnExpected_WhenGivenNull() {
		// when
		User user = crowdRestClient.getUserByToken(null);

		// then
		assertNull(user);
	}

	@Test
	void getUserByToken_ShouldReturnExpected_WhenUserNotFound() {
		// given
		givenUserUnknown();

		// when
		User user = crowdRestClient.getUserByToken("unknown");

		// then
		assertNull(user);
	}

	@Test
	void getUserByToken_ShouldReturnExpected_WhenUserFound() {
		// given
		User expected = new User();
		expected.setFirstName("test-first-name");

		Session session = new Session();
		session.setUser(expected);
		givenSessionKnown(session);

		// when
		User actual = crowdRestClient.getUserByToken("known");

		// then
		assertEquals(expected, actual);
	}

	@Test
	void getUserByToken_ShouldReturnExpected_WhenRolesFound() {
		// given
		User expected = new User();
		expected.setFirstName("test-first-name");
		expected.setLogin("test-username");

		Session session = new Session();
		session.setUser(expected);
		givenSessionKnown(session);

		givenGroupsFound(new GroupsCollection().setGroups(List.of(new Group().setName("admin"))));

		// when
		User actual = crowdRestClient.getUserByToken("known");

		// then
		assertEquals(1, actual.getRoles().size());
		assertEquals("ROLE_admin", actual.getRoles().iterator().next());
	}

	@Test
	void getUserRoles_ShouldReturnExpected_WhenGivenNull() {
		// when
		List<String> result = crowdRestClient.getUserRoles(null);

		// then
		assertEquals(0, result.size());
	}

	@Test
	void getUserRoles_ShouldReturnExpected_WhenUsernameUnknown() {
		// given
		givenUserUnknown();

		// when
		List<String> result = crowdRestClient.getUserRoles("unknown");

		// then
		assertEquals(0, result.size());
	}

	@Test
	void getUserRoles_ShouldReturnExpected_WhenUsernameKnown() {
		// given
		givenUserUnknown();
		givenGroupsFound(new GroupsCollection().setGroups(List.of(new Group().setName("admin"))));

		// when
		List<String> result = crowdRestClient.getUserRoles("known");

		// then
		assertEquals(1, result.size());
		assertEquals("ROLE_admin", result.iterator().next());
	}

	@Test
	void searchUsersByGroup_ShouldReturnExpected_WhenGivenNullGroupName() {
		// when
		List<User> result = crowdRestClient.searchUsersByGroup(null, null, 0, 10);

		// then
		assertEquals(0, result.size());
	}

	@Test
	void searchUsersByGroup_ShouldReturnExpected_WhenGroupNotFound() {
		// given
		givenUsersNotFound();

		// when
		List<User> result = crowdRestClient.searchUsersByGroup(null, null, 0, 10);

		// then
		assertEquals(0, result.size());
	}

	@Test
	void searchUsersByGroup_ShouldReturnExpected_WhenGroupFound() {
		// given
		givenUsersFound(new UsersCollection().setUsers(List.of(new User().setLogin("test-username"))));
		givenUserKnown(new User().setLogin("test-username"));

		// when
		List<User> result = crowdRestClient.searchUsersByGroup("test-group-name", null, 0, 10);

		// then
		assertEquals(1, result.size());
		assertEquals("test-username", result.iterator().next().getLogin());
	}

	@Test
	void searchUsersByGroup_ShouldReturnExpected_WhenGroupAndUsernameFound() {
		// given
		givenUsersFound(new UsersCollection().setName("test-username"));
		givenUserKnown(new User().setLogin("test-username").setEmail("test-email"));

		// when
		List<User> result = crowdRestClient.searchUsersByGroup("test-group-name", "test-username", 0, 10);

		// then
		assertEquals(1, result.size());
		assertEquals("test-username", result.iterator().next().getLogin());
		assertNull(result.iterator().next().getEmail()); // Hidden
	}

	@Test
	void invalidateToken_ShouldReturnExpected_WhenGivenNull() {
		// when
		boolean success = crowdRestClient.invalidateToken(null);

		// then
		assertFalse(success);
	}

	@Test
	void invalidateToken_ShouldReturnExpected_WhenTokenUnknown() {
		// given
		givenTokenUnknown();

		// when
		boolean success = crowdRestClient.invalidateToken("unknown");

		// then
		assertFalse(success);
	}

	@Test
	void invalidateToken_ShouldReturnExpected_WhenTokenKnown() {
		// when
		boolean success = crowdRestClient.invalidateToken("known");

		// then
		assertTrue(success);
	}

	private void givenUnexpectedResponseFormat() {
		// when
		when(restTemplate.postForObject(anyString(), anyMap(), eq(Object.class))).thenReturn(new ArrayList<>());
	}

	private void givenResponseWithoutToken() {
		// when
		when(restTemplate.postForObject(anyString(), anyMap(), eq(Object.class))).thenReturn(new HashMap<>());
	}

	private void givenAuthCorrect(Session session) {
		// when
		when(restTemplate.postForObject(anyString(), anyMap(), eq(Session.class))).thenReturn(session);
	}

	private void givenAuthIncorrect() {
		// when
		when(restTemplate.postForObject(anyString(), anyMap(), eq(Object.class))).thenThrow(RestClientException.class);
	}

	private void givenUserUnknown() {
		// when
		when(restTemplate.getForObject(anyString(), eq(Object.class), anyMap())).thenThrow(RestClientException.class);
		when(restTemplate.getForObject(anyString(), eq(Map.class), anyMap())).thenThrow(RestClientException.class);
		when(restTemplate.getForObject(anyString(), eq(GroupsCollection.class), anyMap())).thenThrow(RestClientException.class);
	}

	private void givenUserKnown(User user) {
		// when
		when(restTemplate.getForObject(anyString(), eq(User.class), anyMap())).thenReturn(user);
	}

	private void givenSessionKnown(Session session) {
		// when
		when(restTemplate.getForObject(anyString(), eq(Session.class), anyMap())).thenReturn(session);
	}

	private void givenGroupsFound(GroupsCollection groups) {
		// when
		when(restTemplate.getForObject(anyString(), eq(GroupsCollection.class), anyMap())).thenReturn(groups);
	}

	private void givenUsersNotFound() {
		// when
		when(restTemplate.getForObject(anyString(), eq(UsersCollection.class), anyMap())).thenThrow(RestClientException.class);
	}

	private void givenUsersFound(UsersCollection users) {
		// when
		when(restTemplate.getForObject(anyString(), eq(UsersCollection.class), anyMap())).thenReturn(users);
	}

	private void givenTokenUnknown() {
		// when
		Mockito.doThrow(new RestClientException("Token unknown")).when(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), any(), eq(Object.class));
	}
}