package org.snomed.ims.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.ims.domain.User;
import org.snomed.ims.domain.UserInformationUpdateRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PropertyFileIdentityProviderTest {

    private PropertyFileIdentityProvider identityProvider;

    @BeforeEach
    void setUp() throws IOException {
        identityProvider = new PropertyFileIdentityProvider("src/test/resources");
    }

    @Test
    void authenticate_ValidCredentials_ReturnsToken() {
        String username = "testUser";
        String password = "testPass";
        String token = identityProvider.authenticate(username, password);
        assertNotNull(token, "Token should not be null for valid credentials");
    }

    @Test
    void authenticate_InvalidCredentials_ReturnsNull() {
        String username = "testUser";
        String password = "wrongPass";
        String token = identityProvider.authenticate(username, password);
        assertNull(token, "Token should be null for invalid credentials");
    }

    @Test
    void getUser_ExistingUser_ReturnsUser() {
        String username = "testUser";
        User user = identityProvider.getUser(username);
        assertNotNull(user, "User should be retrieved for existing username");
        assertEquals(username, user.getLogin(), "Username should match");
    }

    @Test
    void getUser_NonExistingUser_ReturnsNull() {
        String username = "nonExistentUser";
        User user = identityProvider.getUser(username);
        assertNull(user, "User should be null for non-existing username");
    }

    @Test
    void getUserByToken_ValidToken_ReturnsUser() {
        String username = "testUser";
        String password = "testPass";
        String token = identityProvider.authenticate(username, password);
        User user = identityProvider.getUserByToken(token);
        assertNotNull(user, "User should be retrieved for valid token");
        assertEquals(username, user.getLogin(), "Username should match");
    }

    @Test
    void getUserByToken_InvalidToken_ReturnsNull() {
        String token = UUID.randomUUID().toString();
        User user = identityProvider.getUserByToken(token);
        assertNull(user, "User should be null for invalid token");
    }

    @Test
    void getUserRoles_ExistingUser_ReturnsRoles() {
        String username = "testUser";
        List<String> roles = identityProvider.getUserRoles(username);
        assertNotNull(roles, "Roles should not be null for existing user");
        assertFalse(roles.isEmpty(), "Roles should not be empty");
    }

    @Test
    void getUserRoles_NonExistingUser_ReturnsEmptyList() {
        String username = "nonExistentUser";
        List<String> roles = identityProvider.getUserRoles(username);
        assertTrue(roles.isEmpty(), "Roles should be empty for non-existing user");
    }

    @Test
    void searchUsersByGroup_ValidGroup_ReturnsUsers() {
        String groupName = "test-group1";
        String username = null;
        List<User> users = identityProvider.searchUsersByGroup(null, groupName, username, 10, 0);
        assertNotNull(users, "Users list should not be null");
        assertFalse(users.isEmpty(), "Users list should contain users");
    }

    @Test
    void invalidateToken_ValidToken_ReturnsTrue() {
        String username = "testUser";
        String password = "testPass";
        String token = identityProvider.authenticate(username, password);
        boolean result = identityProvider.invalidateToken(token);
        assertTrue(result, "Token invalidation should return true for valid token");
        assertNull(identityProvider.getUserByToken(token), "User should be null after token invalidation");
    }

    @Test
    void invalidateToken_InvalidToken_ReturnsFalse() {
        String token = UUID.randomUUID().toString();
        boolean result = identityProvider.invalidateToken(token);
        assertTrue(result, "Token invalidation can also return true");
    }

    @Test
    void updateUser_ThrowsUnsupportedOperationException() {
        User user = new User();
        UserInformationUpdateRequest request = new UserInformationUpdateRequest("", "", "");
        String token = UUID.randomUUID().toString();
        assertThrows(UnsupportedOperationException.class, () -> {
            identityProvider.updateUser(user, request, token);
        }, "updateUser should throw UnsupportedOperationException");
    }

    @Test
    void resetUserPassword_ThrowsUnsupportedOperationException() {
        String username = "testUser";
        String newPassword = "newPass";
        assertThrows(UnsupportedOperationException.class, () -> {
            User user = new User();
            user.setLogin(username);
            identityProvider.resetUserPassword(user, newPassword);
        }, "resetUserPassword should throw UnsupportedOperationException");
    }
}
