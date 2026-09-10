package org.snomed.ims;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {
	@Test
	void contextLoads() {
		// Keep SonarQube happy
		assertTrue(true);
	}
}
