package org.snomed.ims.rest;

import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.User;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserControllerTest extends IntegrationTest {
	@Test
	void getUser_ShouldUseUserViewFieldNames_NotRawJsonPropertyNames() throws Exception {
		// given
		User user = new User();
		user.setLogin("test-username-1");
		user.setFirstName("Test First 1");
		user.setLastName("Test Last 1");
		user.setDisplayName("Test Display 1");
		user.setActive(true);

		when(identityProvider.getUser("test-username-1")).thenReturn(user);

		// when
		ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/user").param("username", "test-username-1"));
		int status = getStatus(resultActions);
		JsonNode body = getBodyJson(resultActions);

		// then
		assertEquals(200, status);

		// assert (expected)
		assertTrue(body.has("login"));
		assertTrue(body.has("firstName"));
		assertTrue(body.has("lastName"));
		assertTrue(body.has("displayName"));
		assertTrue(body.has("active"));
		assertTrue(body.has("username"));

		// assert (unexpected)
		assertFalse(body.has("name"));
		assertFalse(body.has("first-name"));
		assertFalse(body.has("last-name"));
		assertFalse(body.has("display-name"));
		assertFalse(body.has("key"));
	}
}
