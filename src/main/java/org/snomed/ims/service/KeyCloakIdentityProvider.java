package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.domain.User;
import org.snomed.ims.domain.UserInformationUpdateRequest;
import org.snomed.ims.domain.keycloak.KeyCloakGroup;
import org.snomed.ims.domain.keycloak.KeyCloakUser;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class KeyCloakIdentityProvider implements IdentityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyCloakIdentityProvider.class);
    private static final String ADMIN_REALMS = "/admin/realms/";
    private static final String REALMS = "/realms/";
    private static final String USERS = "/users/";
    // Admin API path fragments (avoid string duplication for Sonar)
    private static final String ADMIN_CLIENTS_BASE = "/clients";
    private static final String ADMIN_CLIENTS_SLASH = "/clients/";
    private static final String ADMIN_ROLES_BASE = "/roles";
    private static final String ADMIN_ROLES_SLASH = "/roles/";
    private static final String ADMIN_USERS_COLLECTION = "/users";
    private static final String ADMIN_COMPOSITES = "/composites";
    private static final String ADMIN_ROLES_BY_ID = "/roles-by-id/";
    private static final String ADMIN_GROUPS_BASE = "/groups";
    private static final String ADMIN_GROUPS_SLASH = "/groups/";
    private static final String QUERY_SEARCH = "?search=";
    private static final String QUERY_EXACT_TRUE = "&exact=true";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String SCOPE = "scope";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String TOKEN = "token";
    public static final String ROLES = "roles";
    public static final String OPENID_PROFILE_EMAIL = "openid profile email";
    public static final String PROTOCOL_OPENID_CONNECT_TOKEN = "/protocol/openid-connect/token";

    private final RestTemplate restTemplate;

    private final String keycloakUrl;

    private final String keycloakRealms;

    private final String keycloakClientId;

    private final String keycloakClientSecrete;

    private final String keycloakAdminClientId;

    private final String keycloakAdminClientSecret;

    public KeyCloakIdentityProvider(RestTemplate restTemplate, String keycloakUrl, String keycloakRealms, String keycloakClientId, String keycloakClientSecrete, String keycloakAdminClientId, String keycloakAdminClientSecret) {
        this.restTemplate = restTemplate;
        this.keycloakUrl = keycloakUrl;
        this.keycloakRealms = keycloakRealms;
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecrete = keycloakClientSecrete;
        this.keycloakAdminClientId = keycloakAdminClientId;
        this.keycloakAdminClientSecret = keycloakAdminClientSecret;
        
        LOGGER.info("KeyCloakIdentityProvider initialized with:");
        LOGGER.info("  - keycloakUrl: {}", keycloakUrl);
        LOGGER.info("  - keycloakRealms: {}", keycloakRealms);
        LOGGER.info("  - keycloakClientId: {}", keycloakClientId);
        LOGGER.info("  - keycloakClientSecret: {}", keycloakClientSecrete != null ? "***" : "null");
        LOGGER.info("  - keycloakAdminClientId: {}", keycloakAdminClientId);
        LOGGER.info("  - keycloakAdminClientSecret: {}", keycloakAdminClientSecret != null ? "***" : "null");
        
        // Validate required configuration
        if (keycloakUrl == null || keycloakUrl.isEmpty()) {
            LOGGER.error("keycloakUrl is null or empty - this will cause all requests to fail");
        }
        if (keycloakRealms == null || keycloakRealms.isEmpty()) {
            LOGGER.error("keycloakRealms is null or empty - this will cause all requests to fail");
        }
        if (keycloakAdminClientId == null || keycloakAdminClientId.isEmpty()) {
            LOGGER.error("keycloakAdminClientId is null or empty - admin API calls will fail");
        }
        if (keycloakAdminClientSecret == null || keycloakAdminClientSecret.isEmpty()) {
            LOGGER.error("keycloakAdminClientSecret is null or empty - admin API calls will fail");
        }
        
        // Log constructed URLs for debugging
        logConstructedUrls();
    }

    // Add this method after the constructor for debugging
    private void logConstructedUrls() {
        LOGGER.info("Constructed URLs for debugging:");
        LOGGER.info("  - Admin token URL: {}{}{}/protocol/openid-connect/token", 
            keycloakUrl, REALMS, keycloakRealms);
        LOGGER.info("  - User groups URL: {}{}{}{}/groups", 
            keycloakUrl, ADMIN_REALMS, keycloakRealms, USERS);
        LOGGER.info("  - Group members URL: {}{}{}/groups/{{groupId}}/members", 
            keycloakUrl, ADMIN_REALMS, keycloakRealms);
    }

    @Override
    public String authenticate(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        try {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add(GRANT_TYPE, "password");
            map.add(SCOPE, "openid");
            map.add(CLIENT_ID, this.keycloakClientId);
            map.add(CLIENT_SECRET, this.keycloakClientSecrete);
            map.add(USERNAME, username);
            map.add(PASSWORD, password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            Map<String, String> response = restTemplate.postForObject(REALMS + this.keycloakRealms + PROTOCOL_OPENID_CONNECT_TOKEN, request, HashMap.class);
            if (response == null) {
                return null;
            }
            return response.getOrDefault(ACCESS_TOKEN, "");
        } catch (Exception e) {
            LOGGER.error("ed680e99-d64b-4852-8006-7b7481890590 Failed to authenticate", e);
            return null;
        }
    }

    @Override
    public User getUser(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        try {
            String adminToken = getAdminToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<List<KeyCloakUser>> response = restTemplate.exchange(
                    ADMIN_REALMS + this.keycloakRealms + "/users?exact=true&username=" + username,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<KeyCloakUser> keyCloakUsers = response.getBody();
            return !CollectionUtils.isEmpty(keyCloakUsers) ? toUser(keyCloakUsers.get(0)) : null;
        } catch (Exception e) {
            LOGGER.error("fa34d4a5-2739-467a-a350-76f22bc463fb Failed to get user", e);
            return null;
        }
    }

    @Override
    @Cacheable(value = "accountCache", key = "#token", unless = "#result == null")
    public User getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return introspectToken(token);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(String username) {
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String adminToken = getAdminToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<KeyCloakUser>> response = restTemplate.exchange(
                    ADMIN_REALMS + this.keycloakRealms + "/users?exact=true&username=" + username,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<KeyCloakUser> users = response.getBody();
            if (CollectionUtils.isEmpty(users)) return Collections.emptyList();

            KeyCloakUser user = users.get(0);
            ResponseEntity<Map> roleMappingsResponse = restTemplate.exchange(
                    ADMIN_REALMS + this.keycloakRealms + USERS + user.getId() + "/role-mappings",
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> roleMappings = roleMappingsResponse.getBody();
            if (roleMappings == null) {
                return Collections.emptyList();
            }

            Map<String, Object> clientMappings = (HashMap) roleMappings.get("clientMappings");
            if (clientMappings == null) {
                return Collections.emptyList();
            }

            List<String> roles = new ArrayList<>();
            for (Map.Entry<String, Object> entry : clientMappings.entrySet()) {
                Map<String, Object> value = (HashMap) entry.getValue();
                List<Object> mappings = (ArrayList) value.get("mappings");
                if (mappings != null) {
                    for (Object object : mappings) {
                        Map<String, String> map = (HashMap) object;
                        roles.add(AuthoritiesConstants.ROLE_PREFIX + map.get("name"));
                    }
                }
            }
            return roles;
        } catch (Exception e) {
            LOGGER.error("97a939d9-6ac6-4db8-954e-3c4ea425d95e Failed to get user's roles", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<User> searchUsersByGroup(String currentUserId, String groupName, String username, int maxResults, int startAt) {
        if (groupName == null || groupName.isEmpty()) {
            LOGGER.debug("Group name is null or empty, returning empty list");
            return Collections.emptyList();
        }
        
        LOGGER.debug("Searching for users in group: {}, currentUserId: {}, username filter: {}, maxResults: {}, startAt: {}", 
            groupName, currentUserId, username, maxResults, startAt);
        
        String adminToken = getAdminToken();
        if (adminToken == null || adminToken.isEmpty()) {
            LOGGER.error("Cannot search users by group - no admin token available");
            return Collections.emptyList();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        
        try {
            // Since we have admin credentials (adminToken), we can search any group directly
            // No need to check current user's group membership when using admin access
            LOGGER.debug("Using admin credentials to search for group directly: {}", groupName);
            List<KeyCloakGroup> keyCloakGroups = findGroupByName(groupName, requestEntity);
            
            if (CollectionUtils.isEmpty(keyCloakGroups)) {
                LOGGER.debug("Group not found: {}. Falling back to role search.", groupName);
                List<User> usersByRole = getUsersForRole(groupName, username, requestEntity);
                if (!CollectionUtils.isEmpty(usersByRole)) {
                    if (startAt >= 0 && startAt < usersByRole.size()) {
                        int toIndex = Math.min(startAt + maxResults, usersByRole.size());
                        List<User> paginatedUsers = usersByRole.subList(startAt, toIndex);
                        LOGGER.debug("Returning paginated users by role: {} to {} (total: {})", startAt, toIndex - 1, paginatedUsers.size());
                        return paginatedUsers;
                    }
                    LOGGER.debug("No users found after pagination for role search");
                    return Collections.emptyList();
                }
                return Collections.emptyList();
            }
            
            LOGGER.debug("Found {} groups for admin search", keyCloakGroups.size());
            keyCloakGroups.forEach(group -> LOGGER.debug("Group: {} (ID: {})", group.getName(), group.getId()));
            
            List<User> users = getUsersForGroup(groupName, username, keyCloakGroups, requestEntity);
            LOGGER.debug("Retrieved {} users for group: {}", users.size(), groupName);
            
            if (startAt >= 0 && startAt < users.size()) {
                int toIndex = Math.min(startAt + maxResults, users.size());
                List<User> paginatedUsers = users.subList(startAt, toIndex);
                LOGGER.debug("Returning paginated users: {} to {} (total: {})", startAt, toIndex - 1, paginatedUsers.size());
                return paginatedUsers;
            }
            
            LOGGER.debug("No users found after pagination");
            return Collections.emptyList();
        } catch (Exception e) {
            LOGGER.error("620cdd4c-f4c4-4105-8ebd-96b1925df746 Failed to get users by group name. Group: {}, CurrentUserId: {}, Error: {}", 
                groupName, currentUserId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @CacheEvict(value = "accountCache", key = "#token")
    public boolean invalidateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add(TOKEN, token);
            map.add(CLIENT_ID, this.keycloakClientId);
            map.add(CLIENT_SECRET, this.keycloakClientSecrete);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            restTemplate.postForObject(REALMS + this.keycloakRealms + "/protocol/openid-connect/revoke", request, Void.class);
            return true;
        } catch (Exception e) {
            LOGGER.error("e1e0f376-0e60-44ea-9d85-238e285d3e6a Failed to invalidate token", e);
            return false;
        }
    }

    @Override
    @CacheEvict(value = "accountCache", key = "#token")
    public User updateUser(User user, UserInformationUpdateRequest request, String token) {
        Map<String, String> updatedFields = new HashMap<>();
        updatedFields.put(EMAIL, user.getEmail());
        if (request.firstName() != null) {
            updatedFields.put("firstName", request.firstName());
        }
        if (request.lastName() != null) {
            updatedFields.put("lastName", request.lastName());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(updatedFields, headers);

        restTemplate.exchange(
                REALMS + this.keycloakRealms + "/account/",
                HttpMethod.POST,
                entity,
                Void.class
        );
        return this.getUser(user.getLogin());
    }

    @Override
    public void resetUserPassword(User user, String newPassword) {
        throw new UnsupportedOperationException("Password reset is not supported via API.");
    }

    @Override
    public String buildAuthorizationUrl(String redirectUri, boolean promptNone) {
        LOGGER.debug("Building authorization URL");
        LOGGER.debug("  - Redirect URI: {}", redirectUri);
        LOGGER.debug("  - Prompt None: {}", promptNone);
        LOGGER.debug("  - Client ID: {}", keycloakClientId);
        LOGGER.debug("  - Keycloak URL: {}", keycloakUrl);
        LOGGER.debug("  - Realm: {}", keycloakRealms);
        
        StringBuilder url = new StringBuilder();
        url.append(keycloakUrl)
                .append(REALMS)
                .append(keycloakRealms)
                .append("/protocol/openid-connect/auth")
                .append("?client_id=").append(URLEncoder.encode(keycloakClientId, StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .append("&response_type=code")
                .append("&scope=").append(URLEncoder.encode(OPENID_PROFILE_EMAIL, StandardCharsets.UTF_8));
        if (promptNone) {
            url.append("&prompt=none");
        }
        
        String finalUrl = url.toString();
        LOGGER.debug("Built authorization URL: {}", finalUrl);
        return finalUrl;
    }



    /**
     * Introspect a lightweight JWT token to get user information
     * @param token the lightweight JWT token to introspect
     * @return User object with user information, or null if token is invalid
     */
    public User introspectToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add(TOKEN, token);
            form.add(CLIENT_ID, this.keycloakClientId);
            form.add(CLIENT_SECRET, this.keycloakClientSecrete);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/jwt"); // Request JWT claim in response
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

            String introspectUrl = keycloakUrl + REALMS + this.keycloakRealms + "/protocol/openid-connect/token/introspect";
            LOGGER.debug("Introspecting lightweight JWT token at: {}", introspectUrl);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    introspectUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body == null) {
                LOGGER.warn("Token introspection response body is null");
                return null;
            }
            
            // Log the full introspection response for debugging
            LOGGER.debug("Token introspection response body: {}", body);
            
            // Check if token is active
            Boolean active = (Boolean) body.get("active");
            if (active == null || !active) {
                LOGGER.debug("Token is not active");
                return null;
            }
            
            // Extract user information from introspection response
            String userId = (String) body.get("sub"); // Extract user ID from subject field
            String username = (String) body.get("preferred_username");
            String email = (String) body.get("email");
            String firstName = (String) body.get("given_name");
            String lastName = (String) body.get("family_name");
            
            if (username == null || username.isEmpty()) {
                LOGGER.warn("Token introspection response contains no username");
                return null;
            }
            
            // Create user object
            User user = new User();
            user.setId(userId); // Set the user ID from the token's subject field
            user.setLogin(username);
            user.setEmail(email != null ? email : "");
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setDisplayName(user.getFirstName() + " " + user.getLastName());

            // Log what we're looking for
            LOGGER.debug("Looking for roles in introspection response...");
            LOGGER.debug("Available keys in response: {}", body.keySet());

            Object realmAccess = body.get("realm_access");
            LOGGER.debug("realm_access: {}", realmAccess);

            Object resourceAccess = body.get("resource_access");
            LOGGER.debug("resource_access: {}", resourceAccess);

            Object directRoles = body.get(ROLES);
            LOGGER.debug("Direct roles: {}", directRoles);

            extractRoles(realmAccess, resourceAccess, directRoles, user);
            extractClientAccess(resourceAccess, user);

            LOGGER.debug("Lightweight JWT token introspection successful for user: {}", username);
            return user;
            
        } catch (Exception e) {
            LOGGER.error("Failed to introspect lightweight JWT token", e);
            return null;
        }
    }

    @Override
    public String exchangeCodeForAccessToken(String code, String redirectUri) {
        LOGGER.debug("Exchanging authorization code for access token");
        LOGGER.debug("  - Code: {}...{}", code != null ? code.substring(0, Math.min(8, code.length())) : "",
                code != null ? code.substring(Math.max(0, code.length() - 8)) : "");
        LOGGER.debug("  - Redirect URI: {}", redirectUri);
        LOGGER.debug("  - Client ID: {}", keycloakClientId);
        LOGGER.debug("  - Client Secret: {}", keycloakClientSecrete != null ? "***" : "null");
        
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add(SCOPE, OPENID_PROFILE_EMAIL); // Add scope for roles and user info
        form.add(CLIENT_ID, keycloakClientId);
        form.add(CLIENT_SECRET, keycloakClientSecrete);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        // Build the token exchange URL
        String tokenUrl = keycloakUrl + REALMS + this.keycloakRealms + PROTOCOL_OPENID_CONNECT_TOKEN;
        LOGGER.debug("Making token exchange request to: {}", tokenUrl);
        LOGGER.debug("Request headers: {}", headers);
        LOGGER.debug("Request body parameters:");
        LOGGER.debug("  - grant_type: authorization_code");
        LOGGER.debug("  - code: {}...{}", code != null ? code.substring(0, Math.min(8, code.length())) : "",
                code != null ? code.substring(Math.max(0, code.length() - 8)) : "");
        LOGGER.debug("  - scope: openid profile email");
        LOGGER.debug("  - redirect_uri: {}", redirectUri);
        LOGGER.debug("  - client_id: {}", keycloakClientId);
        LOGGER.debug("  - client_secret: {}", keycloakClientSecrete != null ? "***" : "null");

        try {
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );
            
            LOGGER.debug("Token exchange response status: {}", tokenResponse.getStatusCode());
            LOGGER.debug("Token exchange response headers: {}", tokenResponse.getHeaders());
            
            Map<String, Object> body = tokenResponse.getBody();
            if (body == null) {
                LOGGER.warn("Token exchange response body is null");
                return null;
            }
            
            LOGGER.debug("Token exchange response body keys: {}", body.keySet());
            Object accessToken = body.get(ACCESS_TOKEN);
            
            if (accessToken == null) {
                LOGGER.warn("Token exchange response contains no access_token");
                LOGGER.debug("Full token exchange response: {}", body);
                return null;
            }
            
            LOGGER.debug("Token exchange successful, access token length: {}", accessToken.toString().length());
            return accessToken.toString();
            
        } catch (Exception e) {
            LOGGER.error("Token exchange failed. URL: {}, Client ID: {}, Redirect URI: {}, Error: {}", 
                tokenUrl, keycloakClientId, redirectUri, e.getMessage(), e);
            return null;
        }
    }

    private String getAdminToken() {
        LOGGER.debug("Getting admin token for client ID: {}", keycloakAdminClientId);
        String token = authenticateAsClient(this.keycloakAdminClientId, this.keycloakAdminClientSecret);
        if (token == null || token.isEmpty()) {
            LOGGER.error("Failed to obtain admin token - this will cause all admin API calls to fail");
        } else {
            LOGGER.debug("Admin token obtained successfully, length: {}", token.length());
        }
        return token;
    }
    
    private String authenticateAsClient(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            LOGGER.warn("Admin client credentials are null or empty - username: {}, password: {}", 
                username != null ? "***" : "null", 
                password != null ? "***" : "null");
            return null;
        }
        
        try {
            LOGGER.debug("Attempting to authenticate admin client with ID: {}", username);
            
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add(GRANT_TYPE, "client_credentials");
            map.add(SCOPE, OPENID_PROFILE_EMAIL);
            map.add(CLIENT_ID, username); // Use username as client_id for admin client
            map.add(CLIENT_SECRET, password); // Use password as client_secret for admin client

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // Build the full URL for admin client authentication
            String tokenUrl = keycloakUrl + REALMS + this.keycloakRealms + PROTOCOL_OPENID_CONNECT_TOKEN;
            LOGGER.debug("Making admin client authentication request to: {}", tokenUrl);
            LOGGER.debug("Request headers: {}", headers);
            LOGGER.debug("Request body parameters: grant_type={}, scope={}, client_id={}, client_secret={}", 
                "client_credentials", OPENID_PROFILE_EMAIL, username, "***");

            Map<String, String> response = restTemplate.postForObject(tokenUrl, request, HashMap.class);
            
            if (response == null) {
                LOGGER.warn("Admin client authentication response is null");
                return null;
            }
            
            String accessToken = response.getOrDefault(ACCESS_TOKEN, "");
            if (accessToken.isEmpty()) {
                LOGGER.warn("Admin client authentication response contains no access_token. Response keys: {}", 
                    response.keySet());
                LOGGER.debug("Full response: {}", response);
            } else {
                LOGGER.debug("Admin client authentication successful, token length: {}", accessToken.length());
            }
            
            return accessToken;
        } catch (Exception e) {
            LOGGER.error("Failed to authenticate as admin client. URL: {}, Client ID: {}, Error: {}", 
                keycloakUrl + REALMS + this.keycloakRealms + PROTOCOL_OPENID_CONNECT_TOKEN,
                username, e.getMessage(), e);
            return null;
        }
    }

    private User toUser(KeyCloakUser keyCloakUser) {
        User user = new User();
        user.setId(keyCloakUser.getId());
        user.setLogin(keyCloakUser.getUsername());
        user.setLastName(keyCloakUser.getLastName());
        user.setFirstName(keyCloakUser.getFirstName());
        user.setDisplayName(keyCloakUser.getFirstName() + " " + keyCloakUser.getLastName());
        user.setEmail(keyCloakUser.getEmail());
        user.setRoles(keyCloakUser.getRoles());
        user.setActive(keyCloakUser.isEnabled());

        return user;
    }

    /**
     * Find a group by name using the Keycloak admin API
     * @param groupName the name of the group to find
     * @param requestEntity the HTTP request entity with admin token
     * @return list containing the group if found, empty list otherwise
     */
    private List<KeyCloakGroup> findGroupByName(String groupName, HttpEntity<String> requestEntity) {
        try {
            // Search for groups by name using Keycloak admin API
            String searchGroupUrl = ADMIN_REALMS + this.keycloakRealms + ADMIN_GROUPS_BASE + QUERY_SEARCH + groupName + QUERY_EXACT_TRUE;
            String fullSearchGroupUrl = keycloakUrl + searchGroupUrl;
            LOGGER.debug("Searching for group by name: {} at URL: {}", groupName, fullSearchGroupUrl);
            
            ResponseEntity<List<KeyCloakGroup>> groupResponse = restTemplate.exchange(
                    fullSearchGroupUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            
            List<KeyCloakGroup> groups = groupResponse.getBody();
            if (CollectionUtils.isEmpty(groups)) {
                LOGGER.debug("No group found with name: {}", groupName);
                return Collections.emptyList();
            }
            
            // Filter to exact match since search might return partial matches
            List<KeyCloakGroup> exactMatches = groups.stream()
                    .filter(group -> groupName.equals(group.getName()))
                    .toList();
            
            LOGGER.debug("Found {} exact matches for group name: {}", exactMatches.size(), groupName);
            return exactMatches;
            
        } catch (Exception e) {
            LOGGER.error("Failed to find group by name: {}, Error: {}", groupName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<User> getUsersForGroup(final String groupName, final String username, List<KeyCloakGroup> keyCloakGroups, HttpEntity<String> requestEntity) {
        return keyCloakGroups.stream()
                .filter(group -> group.getName().equals(groupName))
                .flatMap(group -> {
                    LOGGER.debug("Getting members for group: {} (ID: {})", group.getName(), group.getId());
                    
                    String groupMembersUrl = ADMIN_REALMS + this.keycloakRealms + "/groups/" + group.getId() + "/members?max=-1";
                    String fullGroupMembersUrl = keycloakUrl + groupMembersUrl;
                    LOGGER.debug("Making request to get group members from: {}", fullGroupMembersUrl);
                    
                    ResponseEntity<List<KeyCloakUser>> userResponse = restTemplate.exchange(
                            fullGroupMembersUrl,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<>() {
                            }
                    );
                    
                    LOGGER.debug("Group members response status: {}, body size: {}", 
                        userResponse.getStatusCode(), userResponse.getBody() != null ? userResponse.getBody().size() : 0);
                    
                    List<KeyCloakUser> keyCloakUsers = userResponse.getBody();
                    if (CollectionUtils.isEmpty(keyCloakUsers)) {
                        LOGGER.debug("No members found for group: {}", group.getName());
                        return Stream.empty();
                    }
                    
                    LOGGER.debug("Found {} members in group: {}", keyCloakUsers.size(), group.getName());
                    
                    return keyCloakUsers.stream()
                            .filter(KeyCloakUser::isEnabled)
                            .filter(u -> !StringUtils.hasLength(username) || u.getUsername().contains(username))
                            .map(u -> {
                                User user = toUser(u);
                                user.setEmail(null);
                                LOGGER.debug("Mapped user: {} (enabled: {})", u.getUsername(), u.isEnabled());
                                return user;
                            });
                }).distinct().toList();
    }

    private void extractClientAccess(Object resourceAccess, User user) {
        // Extract client access information
        List<String> clientAccess = new ArrayList<>();
        if (resourceAccess instanceof Map) {
            Map<?, ?> resourceAccessMap = (Map<?, ?>) resourceAccess;
            for (Map.Entry<?, ?> entry : resourceAccessMap.entrySet()) {
                String clientName = entry.getKey().toString();
                clientAccess.add(clientName);
            }
        }
        LOGGER.debug("Client access: {}", clientAccess);
        user.setClientAccess(clientAccess);
    }

    private void extractRoles(Object realmAccess, Object resourceAccess, Object directRoles, User user) {
        // Extract ALL roles from realm_access and resource_access
        List<String> roles = new ArrayList<>();

        extractRolesFromRealmAccess(realmAccess, roles);
        extractRolesFromResourceAccess(resourceAccess, roles);
        extractDirectRoles(directRoles, roles);

        LOGGER.debug("Final extracted roles: {}", roles);
        user.setRoles(roles);
    }

    private void extractDirectRoles(Object directRoles, List<String> roles) {
        // 3. Also check for roles directly in the response (some Keycloak versions put them here)
        if (directRoles instanceof List) {
            for (Object role : (List<?>) directRoles) {
                if (role instanceof String s) {
                    roles.add(AuthoritiesConstants.ROLE_PREFIX + s);
                }
            }
        }
    }

    private void extractRolesFromResourceAccess(Object resourceAccess, List<String> roles) {
        // 2. Extract ALL client-specific roles from resource_access
        if (!(resourceAccess instanceof Map)) return;
        Map<?, ?> resourceAccessMap = (Map<?, ?>) resourceAccess;
        LOGGER.debug("Found {} client entries in resource_access", resourceAccessMap.size());

        for (Map.Entry<?, ?> entry : resourceAccessMap.entrySet()) {
            String clientName = entry.getKey().toString();
            Object clientAccess = entry.getValue();
            LOGGER.debug("Processing client: {}", clientName);

            if (!(clientAccess instanceof Map)) continue;
            Object clientRoles = ((Map<?, ?>) clientAccess).get(ROLES);
            LOGGER.debug("Client {} roles: {}", clientName, clientRoles);

            if (clientRoles instanceof List) {
                for (Object role : (List<?>) clientRoles) {
                    if (role instanceof String) {
                        // Add ROLE_ prefixed role name
                        String roleName = AuthoritiesConstants.ROLE_PREFIX + role;
                        roles.add(roleName);
                    }
                }
            }
        }
    }

    private void extractRolesFromRealmAccess(Object realmAccess, List<String> roles) {
        // 1. Extract realm-level roles
        if (realmAccess instanceof Map) {
            Object rolesObj = ((Map<?, ?>) realmAccess).get(ROLES);
            LOGGER.debug("realm_access.roles: {}", rolesObj);
            extractDirectRoles(rolesObj, roles);
        }
    }

    private List<User> getUsersForRole(final String roleOrPrefixedRoleName, final String username, HttpEntity<String> requestEntity) {
        try {
            String roleName = roleOrPrefixedRoleName.startsWith(AuthoritiesConstants.ROLE_PREFIX)
                    ? roleOrPrefixedRoleName.substring(AuthoritiesConstants.ROLE_PREFIX.length())
                    : roleOrPrefixedRoleName;

            String encodedRole = URLEncoder.encode(roleName, StandardCharsets.UTF_8);

            // 1) Try realm role users endpoint
            String realmRoleUsersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_ROLES_SLASH + encodedRole + ADMIN_USERS_COLLECTION;
            LOGGER.debug("Trying realm role users endpoint: {}", realmRoleUsersUrl);
            List<KeyCloakUser> realmRoleUsers = fetchUsers(realmRoleUsersUrl, requestEntity);

            // If found, map and return
            if (!CollectionUtils.isEmpty(realmRoleUsers)) {
                return mapAndFilterUsers(realmRoleUsers, username);
            }

            // 2) Try client role users endpoint for our configured client
            List<User> clientRoleUsers = getUsersForClientRole(username, requestEntity, encodedRole);
            if (!CollectionUtils.isEmpty(clientRoleUsers)) {
                return clientRoleUsers;
            }

            // 3) Fallback: search across all clients for a matching role name
            List<User> acrossClients = getUsersForRoleAcrossAllClients(roleName, username, requestEntity);
            if (!CollectionUtils.isEmpty(acrossClients)) {
                return acrossClients;
            }

            // 4) Final fallback: fetch users by groups that have the role assigned (group role mappings)
            return getUsersViaGroupRoleAssignments(roleName, username, requestEntity);
        } catch (Exception e) {
            LOGGER.error("Failed to search users by role: {}", roleOrPrefixedRoleName, e);
        }
        return Collections.emptyList();
    }

    private List<User> getUsersForClientRole(String username, HttpEntity<String> requestEntity, String encodedRole) {
        try {
            String clientInternalId = resolveClientInternalId(this.keycloakClientId, requestEntity);
            if (clientInternalId != null) {
                String clientRoleUsersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientInternalId + ADMIN_ROLES_SLASH + encodedRole + ADMIN_USERS_COLLECTION;
                LOGGER.debug("Trying client role users endpoint: {}", clientRoleUsersUrl);
                List<KeyCloakUser> clientRoleUsers = fetchUsers(clientRoleUsersUrl, requestEntity);
                if (!CollectionUtils.isEmpty(clientRoleUsers)) {
                    return mapAndFilterUsers(clientRoleUsers, username);
                }
            } else {
                LOGGER.debug("Could not resolve internal client ID for clientId: {}", this.keycloakClientId);
            }
        } catch (Exception e) {
            LOGGER.debug("Error resolving or querying client role users: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<User> mapAndFilterUsers(List<KeyCloakUser> users, String username) {
        return users.stream()
                .filter(KeyCloakUser::isEnabled)
                .filter(u -> !StringUtils.hasLength(username) || u.getUsername().contains(username))
                .map(u -> {
                    User user = toUser(u);
                    user.setEmail(null);
                    return user;
                })
                .distinct()
                .toList();
    }

    private List<User> getUsersForRoleAcrossAllClients(String roleName, String username, HttpEntity<String> requestEntity) {
        try {
            LOGGER.debug("Searching all clients for role: {}", roleName);
            String encodedRole = URLEncoder.encode(roleName, StandardCharsets.UTF_8);

            List<User> aggregated = new ArrayList<>();
            int pageSize = 100;
            for (int first = 0; ; first += pageSize) {
                List<Map<String, Object>> clients = fetchClientsPage(first, pageSize, requestEntity);
                if (CollectionUtils.isEmpty(clients)) {
                    break;
                }
                for (Map<String, Object> client : clients) {
                    aggregated.addAll(aggregateUsersForClient(client, roleName, encodedRole, username, requestEntity));
                }
            }

            return aggregated.stream().distinct().toList();
        } catch (Exception e) {
            LOGGER.debug("Error searching role across all clients: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> fetchClientsPage(int first, int pageSize, HttpEntity<String> requestEntity) {
        String clientsUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_BASE + "?first=" + first + "&max=" + pageSize;
        return fetchListOfMaps(clientsUrl, requestEntity);
    }

    private List<User> aggregateUsersForClient(Map<String, Object> client,
                                               String roleName,
                                               String encodedRole,
                                               String username,
                                               HttpEntity<String> requestEntity) {
        Object idValue = client.get("id");
        if (idValue == null) {
            return Collections.emptyList();
        }
        String clientIdInternal = idValue.toString();

        String rolesSearchUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientIdInternal + ADMIN_ROLES_BASE + QUERY_SEARCH + encodedRole;
        List<Map<String, Object>> roles = fetchListOfMaps(rolesSearchUrl, requestEntity);
        boolean hasExactMatch = roles.stream().anyMatch(r -> roleName.equals(r.get("name")));
        if (!hasExactMatch) {
            return Collections.emptyList();
        }

        String usersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientIdInternal + ADMIN_ROLES_SLASH + encodedRole + ADMIN_USERS_COLLECTION;
        LOGGER.debug("Found matching role on client {}. Fetching users via: {}", clientIdInternal, usersUrl);
        List<KeyCloakUser> clientRoleUsers = fetchUsers(usersUrl, requestEntity);
        if (!CollectionUtils.isEmpty(clientRoleUsers)) {
            return mapAndFilterUsers(clientRoleUsers, username);
        }

        // If no direct users, expand composites and aggregate users from child roles
        LOGGER.debug("No direct users found for role {} on client {}. Expanding composites.", encodedRole, clientIdInternal);
        List<User> aggregated = new ArrayList<>();

        // 1) Client role composites (returns both realm and client composites)
        String clientCompositeUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientIdInternal + ADMIN_ROLES_SLASH + encodedRole + ADMIN_COMPOSITES;
        List<Map<String, Object>> clientCompositeRoles = fetchListOfMaps(clientCompositeUrl, requestEntity);
        aggregated.addAll(fetchUsersForCompositeRoles(clientCompositeRoles, username, requestEntity));

        // 2) Realm role composites (only applicable if a realm role with this name exists)
        String realmCompositeUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_ROLES_SLASH + encodedRole + ADMIN_COMPOSITES;
        List<Map<String, Object>> realmCompositeRoles = fetchListOfMaps(realmCompositeUrl, requestEntity);
        aggregated.addAll(fetchUsersForCompositeRoles(realmCompositeRoles, username, requestEntity));

        return aggregated.stream().distinct().toList();
    }

    private List<Map<String, Object>> fetchListOfMaps(String url, HttpEntity<String> requestEntity) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch list from {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<User> getUsersViaGroupRoleAssignments(String roleName, String username, HttpEntity<String> requestEntity) {
        try {
            String encodedRole = URLEncoder.encode(roleName, StandardCharsets.UTF_8);
            List<User> realmUsers = getUsersViaRealmRoleAssignments(roleName, encodedRole, username, requestEntity);
            List<User> clientUsers = getUsersViaClientRoleAssignments(roleName, encodedRole, username, requestEntity);
            List<User> combined = new ArrayList<>(realmUsers.size() + clientUsers.size());
            combined.addAll(realmUsers);
            combined.addAll(clientUsers);
            return combined.stream().distinct().toList();
        } catch (Exception e) {
            LOGGER.debug("Error fetching users via group role assignments for role {}: {}", roleName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<User> getUsersViaRealmRoleAssignments(String roleName,
                                                       String encodedRole,
                                                       String username,
                                                       HttpEntity<String> requestEntity) {
        String realmRolesSearch = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_ROLES_BASE + QUERY_SEARCH + encodedRole + QUERY_EXACT_TRUE;
        List<Map<String, Object>> realmRoles = fetchListOfMaps(realmRolesSearch, requestEntity);
        List<User> aggregated = new ArrayList<>();
        for (Map<String, Object> role : realmRoles) {
            if (roleName.equals(role.get("name"))) {
                Object roleId = role.get("id");
                aggregated.addAll(fetchUsersFromGroupsForRoleId(roleId != null ? roleId.toString() : null, username, requestEntity));
            }
        }
        return aggregated;
    }

    private List<User> getUsersViaClientRoleAssignments(String roleName,
                                                        String encodedRole,
                                                        String username,
                                                        HttpEntity<String> requestEntity) {
        List<User> aggregated = new ArrayList<>();
        for (int first = 0; ; first += 100) {
            List<Map<String, Object>> clients = fetchClientsPage(first, 100, requestEntity);
            if (CollectionUtils.isEmpty(clients)) break;
            for (Map<String, Object> client : clients) {
                Object idValue = client.get("id");
                if (idValue == null) continue;
                String clientIdInternal = idValue.toString();
                String rolesSearchUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientIdInternal + ADMIN_ROLES_BASE + QUERY_SEARCH + encodedRole + QUERY_EXACT_TRUE;
                List<Map<String, Object>> roles = fetchListOfMaps(rolesSearchUrl, requestEntity);
                for (Map<String, Object> role : roles) {
                    if (roleName.equals(role.get("name"))) {
                        Object roleId = role.get("id");
                        aggregated.addAll(fetchUsersFromGroupsForRoleId(roleId != null ? roleId.toString() : null, username, requestEntity));
                    }
                }
            }
        }
        return aggregated;
    }

    private List<User> fetchUsersFromGroupsForRoleId(String roleId,
                                                     String username,
                                                     HttpEntity<String> requestEntity) {
        if (roleId == null || roleId.isEmpty()) return Collections.emptyList();

        // Roles-by-id group mappings endpoint
        String groupMappingsUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_ROLES_BY_ID + roleId + ADMIN_GROUPS_BASE;
        List<Map<String, Object>> groups = fetchListOfMaps(groupMappingsUrl, requestEntity);
        if (CollectionUtils.isEmpty(groups)) return Collections.emptyList();

        List<User> aggregated = new ArrayList<>();
        for (Map<String, Object> group : groups) {
            Object groupIdObj = group.get("id");
            if (groupIdObj == null) continue;
            String groupId = groupIdObj.toString();

            // Gather members for this group and all subgroups
            aggregated.addAll(fetchUsersForGroupAndDescendants(groupId, username, requestEntity));
        }
        return aggregated;
    }

    private List<User> fetchUsersForGroupAndDescendants(String groupId,
                                                        String username,
                                                        HttpEntity<String> requestEntity) {
        List<User> aggregated = new ArrayList<>();
        // Members of group
        String membersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_GROUPS_SLASH + groupId + ADMIN_USERS_COLLECTION + "?max=-1";
        List<KeyCloakUser> members = fetchUsers(membersUrl, requestEntity);
        if (!CollectionUtils.isEmpty(members)) {
            aggregated.addAll(mapAndFilterUsers(members, username));
        }

        // Recurse into subgroups
        String childrenUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_GROUPS_SLASH + groupId + "/children";
        List<Map<String, Object>> children = fetchListOfMaps(childrenUrl, requestEntity);
        for (Map<String, Object> child : children) {
            Object childId = child.get("id");
            if (childId != null) {
                aggregated.addAll(fetchUsersForGroupAndDescendants(childId.toString(), username, requestEntity));
            }
        }
        return aggregated;
    }

    private List<User> fetchUsersForCompositeRoles(List<Map<String, Object>> compositeRoles,
                                                   String username,
                                                   HttpEntity<String> requestEntity) {
        if (CollectionUtils.isEmpty(compositeRoles)) {
            return Collections.emptyList();
        }

        List<User> aggregated = new ArrayList<>();
        for (Map<String, Object> role : compositeRoles) {
            Object nameObj = role.get("name");
            Object clientIdObj = role.get("clientRole"); // boolean in KC indicating client role
            String clientInternalIdFromRole = null;
            if (Boolean.TRUE.equals(clientIdObj)) {
                // When role is a client role, resolve its client by "containerId"
                Object containerId = role.get("containerId");
                if (containerId != null) {
                    clientInternalIdFromRole = containerId.toString();
                }
            }
            if (nameObj == null) {
                continue;
            }
            String encodedChildRole = URLEncoder.encode(nameObj.toString(), StandardCharsets.UTF_8);

            String usersUrl;
            if (clientInternalIdFromRole != null) {
                usersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_SLASH + clientInternalIdFromRole + ADMIN_ROLES_SLASH + encodedChildRole + ADMIN_USERS_COLLECTION;
            } else {
                usersUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_ROLES_SLASH + encodedChildRole + ADMIN_USERS_COLLECTION;
            }
            List<KeyCloakUser> childUsers = fetchUsers(usersUrl, requestEntity);
            if (!CollectionUtils.isEmpty(childUsers)) {
                aggregated.addAll(mapAndFilterUsers(childUsers, username));
            }
        }
        return aggregated;
    }

    private List<KeyCloakUser> fetchUsers(String url, HttpEntity<String> requestEntity) {
        try {
            ResponseEntity<List<KeyCloakUser>> userResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            List<KeyCloakUser> body = userResponse.getBody();
            int bodySize = body != null ? body.size() : 0;
            LOGGER.debug("Users response status: {}, body size: {}", userResponse.getStatusCode(), bodySize);
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch users from {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveClientInternalId(String clientId, HttpEntity<String> requestEntity) {
        try {
            String clientsUrl = keycloakUrl + ADMIN_REALMS + this.keycloakRealms + ADMIN_CLIENTS_BASE + "?clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
            LOGGER.debug("Resolving client internal ID via: {}", clientsUrl);
            ResponseEntity<List<Map<String, Object>>> clientResponse = restTemplate.exchange(
                    clientsUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );
            List<Map<String, Object>> clients = clientResponse.getBody();
            if (!CollectionUtils.isEmpty(clients)) {
                Object idValue = clients.get(0).get("id");
                return idValue != null ? idValue.toString() : null;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve client internal ID for {}: {}", clientId, e.getMessage());
        }
        return null;
    }
}
