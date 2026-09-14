package org.snomed.ims.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplateConfigTest {
	private static final String CROWD_BASE = "http://crowd.example.com/rest/usermanagement/1";
	private static final String KEYCLOAK_BASE = "http://keycloak.example.com/auth";

	private RestTemplate crowdRestTemplate() {
		ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
		when(applicationProperties.getCrowdApiUrl()).thenReturn(CROWD_BASE);
		when(applicationProperties.getCrowdApiAppName()).thenReturn("app");
		when(applicationProperties.getCrowdApiAppPassword()).thenReturn("secret");

		return new RestTemplateConfig(applicationProperties).crowdRestTemplate();
	}

	@Test
	void crowdRestTemplate_ShouldResolveRelativePathAgainstBaseUri() {
		// given
		RestTemplate restTemplate = crowdRestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(CROWD_BASE + "/session/test-token"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		// when
		restTemplate.getForObject("/session/{token}", String.class, Map.of("token", "test-token"));

		// then
		server.verify();
	}

	@Test
	void crowdRestTemplate_ShouldStrictlyEncodeUriVariables() {
		// baseUri() installs a DefaultUriBuilderFactory in its default TEMPLATE_AND_VALUES
		// encoding mode, so reserved characters in a username are percent-encoded rather than
		// passed through into the query string. The deprecated rootUri() left RestTemplate's
		// URI_COMPONENT handler in place, which let '&' and '=' through literally.

		// given
		RestTemplate restTemplate = crowdRestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(CROWD_BASE + "/user?username=a%26b%3Dc%20d%2Be"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		// when
		restTemplate.getForObject("/user?username={username}", String.class, Map.of("username", "a&b=c d+e"));

		// then
		server.verify();
	}

	@Test
	void keyCloakRestTemplate_ShouldLeaveAbsoluteUrlsUnchanged() {
		// KeyCloakIdentityProvider builds absolute token/admin URLs; the base URI must not be applied to those.

		// given
		ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
		when(applicationProperties.getKeycloakUrl()).thenReturn(KEYCLOAK_BASE);
		RestTemplate restTemplate = new RestTemplateConfig(applicationProperties).keyCloakRestTemplate();

		String absoluteUrl = "http://other.example.com/realms/test/protocol/openid-connect/token";
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo(absoluteUrl)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		// when
		restTemplate.getForObject(absoluteUrl, String.class);

		// then
		server.verify();
	}
}
