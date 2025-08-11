package org.snomed.ims.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.ims.service.IdentityProvider;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AuthControllerTest extends IntegrationTest {
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final String CALLBACK_ENDPOINT = "/auth/callback";
    private static final String AUTO_LOGIN_ENDPOINT = "/auth/auto";

    @BeforeEach
    void setUp() {
        super.setUp();
        // Reset mocks before each test
        org.mockito.Mockito.reset(identityProvider);
    }

    @Test
    void login_ShouldRedirectToKeycloak_WhenValidRequest() {
        // given
        String returnTo = "/dashboard";
        when(identityProvider.buildAuthorizationUrl(anyString(), eq(false)))
                .thenReturn("http://keycloak:8080/realms/test/protocol/openid-connect/auth?client_id=test-client&redirect_uri=http://localhost:8080/identity-management-service/api/auth/callback&response_type=code&scope=openid%20profile%20email");

        // when
        ResultActions resultActions = get(LOGIN_ENDPOINT + "?returnTo=" + returnTo);
        int status = getStatus(resultActions);
        String body = getBody(resultActions);
        String location = getResponseHeader(resultActions, "Location");

        // then
        if (status != 302) {
            System.out.println("Login test failed - Status: " + status + ", Body: " + body);
        }
        assertEquals(302, status);
        assertTrue(location.contains("state=%2Fdashboard"));
        assertTrue(location.contains("keycloak"));
    }

    @Test
    void login_ShouldReturnError_WhenIdentityProviderFails() {
        // given
        when(identityProvider.buildAuthorizationUrl(anyString(), eq(false)))
                .thenReturn(null);

        // when
        ResultActions resultActions = get(LOGIN_ENDPOINT);
        int status = getStatus(resultActions);

        // then
        assertEquals(500, status);
    }

    @Test
    void autoLogin_ShouldRedirectToKeycloak_WhenValidRequest() {
        // given
        String returnTo = "/dashboard";
        when(identityProvider.buildAuthorizationUrl(anyString(), eq(true)))
                .thenReturn("http://keycloak:8080/realms/test/protocol/openid-connect/auth?client_id=test-client&redirect_uri=http://localhost:8080/identity-management-service/api/auth/callback&response_type=code&scope=openid%20profile%20email&prompt=none");

        // when
        ResultActions resultActions = get(AUTO_LOGIN_ENDPOINT + "?returnTo=" + returnTo);
        int status = getStatus(resultActions);
        String body = getBody(resultActions);
        String location = getResponseHeader(resultActions, "Location");

        // then
        if (status != 302) {
            System.out.println("Auto login test failed - Status: " + status + ", Body: " + body);
        }
        assertEquals(302, status);
        assertTrue(location.contains("prompt=none"));
        assertTrue(location.contains("keycloak"));
    }

    @Test
    void callback_ShouldSetCookieAndRedirect_WhenValidCode() {
        // given
        String code = "valid-auth-code";
        String state = "/dashboard";
        String accessToken = "valid-access-token";
        
        when(identityProvider.exchangeCodeForAccessToken(eq(code), anyString()))
                .thenReturn(accessToken);

        // when
        ResultActions resultActions = get(CALLBACK_ENDPOINT + "?code=" + code + "&state=" + state);
        int status = getStatus(resultActions);
        String body = getBody(resultActions);
        String location = getResponseHeader(resultActions, "Location");

        // then
        if (status != 302) {
            System.out.println("Test failed - Status: " + status + ", Body: " + body);
        }
        assertEquals(302, status);
        assertEquals("/dashboard", location);
    }

    @Test
    void callback_ShouldReturnError_WhenInvalidCode() {
        // given
        String code = "invalid-auth-code";
        String state = "/dashboard";
        
        when(identityProvider.exchangeCodeForAccessToken(eq(code), anyString()))
                .thenReturn(null);

        // when
        ResultActions resultActions = get(CALLBACK_ENDPOINT + "?code=" + code + "&state=" + state);
        int status = getStatus(resultActions);

        // then
        assertEquals(401, status);
    }
}
