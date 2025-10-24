package org.snomed.ims.rest;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;

class VersionControllerTest extends IntegrationTest {
	private static final String GET_VERSION = "/version";

	@Test
	void getAccount_ShouldReturnExpected() {
		// when
		ResultActions resultActions = get(GET_VERSION);
		int status = getStatus(resultActions);
		String body = getBody(resultActions);

		// then
		assertEquals(200, status);
		assertTrue(body.contains("version"));
		assertTrue(body.contains("time"));
	}
}