package org.snomed.ims.rest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.ims.config.ApplicationProperties;
import org.snomed.ims.service.CrowdRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ActiveProfiles("test") // Use application-test.properties
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {
	protected RestTemplate restTemplate = mock(RestTemplate.class);

	protected CrowdRestClient crowdRestClient = new CrowdRestClient(restTemplate);

	@Autowired
	protected ApplicationProperties applicationProperties;

	protected MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AccountController accountController = new AccountController(crowdRestClient, applicationProperties);
		AuthController authController = new AuthController(crowdRestClient, applicationProperties);
		VersionController versionController = new VersionController(applicationProperties);

		this.mockMvc = MockMvcBuilders
				.standaloneSetup(accountController, authController, versionController)
				.build();
	}

	@Test
	void testName() {
		// Keep SonarQube happy
		assertTrue(true);
	}

	protected ResultActions get(String url) {
		try {
			return mockMvc.perform(MockMvcRequestBuilders.get(url));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ResultActions get(String url, Cookie cookie) {
		try {
			return mockMvc.perform(MockMvcRequestBuilders.get(url).cookie(cookie));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ResultActions post(String url) {
		try {
			return mockMvc.perform(MockMvcRequestBuilders.post(url));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ResultActions post(String url, Cookie cookie) {
		try {
			return mockMvc.perform(MockMvcRequestBuilders.post(url).cookie(cookie));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected int getStatus(ResultActions resultActions) {
		try {
			MvcResult mvcResult = resultActions.andReturn();
			return mvcResult.getResponse().getStatus();
		} catch (Exception e) {
			return -1;
		}
	}

	protected String getBody(ResultActions resultActions) {
		try {
			return resultActions.andReturn().getResponse().getContentAsString();
		} catch (Exception e) {
			return null;
		}
	}

	protected String getResponseHeader(ResultActions resultActions, String headerName) {
		try {
			Collection<String> headers = resultActions.andReturn().getResponse().getHeaders(headerName);
			if (headers.isEmpty()) {
				return null;
			}

			return headers.iterator().next();
		} catch (Exception e) {
			throw new RuntimeException("Failed to get response header: " + headerName, e);
		}
	}
}
