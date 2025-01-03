package org.snomed.ims.domain.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserViewTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void serialize_ShouldThrowExpected_WhenGivenNull() {
		// given
		UserView userView = new UserView();

		// then
		assertThrows(IllegalArgumentException.class, () -> userView.serialize(null, null, null));
	}

	@Test
	void serialize_ShouldWriteExpected() throws JsonProcessingException {
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
		String result = OBJECT_MAPPER.writeValueAsString(user);

		// then
		assertEquals("{\"login\":\"test-login\",\"firstName\":\"test-first-name\",\"lastName\":\"test-last-name\",\"email\":\"test-email\",\"displayName\":\"test-display-name\",\"active\":true,\"username\":\"test-login\"}", result);
	}

	@Test
	void serialize_ShouldWriteExpected_WhenGivenRoles() throws JsonProcessingException {
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
		String result = OBJECT_MAPPER.writeValueAsString(user);

		// then
		assertEquals("{\"login\":\"test-login\",\"firstName\":\"test-first-name\",\"lastName\":\"test-last-name\",\"email\":\"test-email\",\"displayName\":\"test-display-name\",\"active\":true,\"username\":\"test-login\",\"roles\":[\"admin\",\"user\",\"project-manager\"]}", result);
	}
}
