package org.snomed.ims.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.User;
import org.snomed.ims.domain.UserView;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserViewTest {
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(User.class, new UserView());
		objectMapper = JsonMapper.builder().addModule(module).build();
	}

	@Test
	void serialize_ShouldWriteExpected() throws Exception {
		// given
		User user = new User();
		user.setLogin("test-login");
		user.setFirstName("test-first-name");
		user.setLastName("test-last-name");
		user.setEmail("test-email");
		user.setDisplayName("test-display-name");
		user.setActive(true);
		user.setLangKey("en"); // Omitted from json

		// when
		String result = objectMapper.writeValueAsString(user);

		// then
		assertEquals("{\"login\":\"test-login\",\"firstName\":\"test-first-name\",\"lastName\":\"test-last-name\",\"email\":\"test-email\",\"displayName\":\"test-display-name\",\"active\":true,\"username\":\"test-login\"}", result);
	}

	@Test
	void serialize_ShouldWriteExpected_WhenGivenRoles() throws Exception {
		// given
		User user = new User();
		user.setLogin("test-login");
		user.setFirstName("test-first-name");
		user.setLastName("test-last-name");
		user.setEmail("test-email");
		user.setDisplayName("test-display-name");
		user.setActive(true);
		user.setLangKey("en");
		user.setRoles(List.of("admin", "user", "project-manager"));

		// when
		String result = objectMapper.writeValueAsString(user);

		// then
		assertEquals("{\"login\":\"test-login\",\"firstName\":\"test-first-name\",\"lastName\":\"test-last-name\",\"email\":\"test-email\",\"displayName\":\"test-display-name\",\"active\":true,\"username\":\"test-login\",\"roles\":[\"admin\",\"user\",\"project-manager\"]}", result);
	}

	@Test
	void serialize_ShouldNotIncludeRawFieldNames() throws Exception {
		// Regression test: ensure response uses UserView field names (login, firstName, etc.)
		// and NOT the @JsonProperty Crowd deserialization names (name, first-name, etc.)
		User user = new User();
		user.setLogin("test-username-1");
		user.setFirstName("Test First 1");
		user.setLastName("Test Last 1");
		user.setDisplayName("Test Display 1");
		user.setActive(true);

		String result = objectMapper.writeValueAsString(user);

		assertTrue(result.contains("\"login\""));
		assertTrue(result.contains("\"firstName\""));
		assertTrue(result.contains("\"lastName\""));
		assertTrue(result.contains("\"username\""));
		assertFalse(result.contains("\"name\""));
		assertFalse(result.contains("\"first-name\""));
		assertFalse(result.contains("\"last-name\""));
		assertFalse(result.contains("\"key\""));
		assertFalse(result.contains("\"display-name\""));
	}
}
