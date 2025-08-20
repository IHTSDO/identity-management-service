package org.snomed.ims.service;

import jakarta.servlet.http.HttpServletRequest;
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
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

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
            map.add("grant_type", "password");
            map.add("scope", "openid");
            map.add(CLIENT_ID, this.keycloakClientId);
            map.add(CLIENT_SECRET, this.keycloakClientSecrete);
            map.add(USERNAME, username);
            map.add(PASSWORD, password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            Map<String, String> response = restTemplate.postForObject(REALMS + this.keycloakRealms + "/protocol/openid-connect/token", request, HashMap.class);
            if (response == null) {
                return null;
            }
            return response.getOrDefault("access_token", "");
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
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add(CLIENT_ID, this.keycloakClientId);
            body.add(CLIENT_SECRET, this.keycloakClientSecrete);
            body.add("token", token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<HashMap> response = restTemplate.exchange(
                    REALMS + this.keycloakRealms + "/protocol/openid-connect/token/introspect",
                    HttpMethod.POST,
                    request,
                    HashMap.class
            );

            Map map = response.getBody();
            if (map == null) return null;

            boolean active = Boolean.parseBoolean(map.get("active").toString());
            if (!active) return null;

            User user = new User();
            user.setId(map.get("sub").toString());
            user.setLogin(map.get("username").toString());
            user.setLastName(map.get("family_name").toString());
            user.setFirstName(map.get("given_name").toString());
            user.setDisplayName(map.get("name").toString());
            user.setEmail(map.get("email").toString());
            user.setAppAudiences((ArrayList) map.get("aud"));
            user.setActive(true);
            setRoleToUser(user, map);

            return user;
        } catch (Exception e) {
            LOGGER.error("fdec3996-b2ef-4811-8e5a-86df6c2bbc25 Failed to get user by token", e);
            return null;
        }
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
            // First API call: Get user's groups
            String userGroupsUrl = ADMIN_REALMS + this.keycloakRealms + USERS + currentUserId + "/groups";
            String fullUserGroupsUrl = keycloakUrl + userGroupsUrl;
            LOGGER.debug("Making request to get user groups from: {}", fullUserGroupsUrl);
            LOGGER.debug("Request headers: Authorization=Bearer ***{}", adminToken.substring(Math.max(0, adminToken.length() - 8)));
            
            ResponseEntity<List<KeyCloakGroup>> groupResponse = restTemplate.exchange(
                    fullUserGroupsUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            
            List<KeyCloakGroup> keyCloakGroups = groupResponse.getBody();
            LOGGER.debug("User groups response status: {}, body size: {}", 
                groupResponse.getStatusCode(), keyCloakGroups != null ? keyCloakGroups.size() : 0);
            
            if (CollectionUtils.isEmpty(keyCloakGroups)) {
                LOGGER.debug("No groups found for user: {}", currentUserId);
                return Collections.emptyList();
            }
            
            LOGGER.debug("Found {} groups for user: {}", keyCloakGroups.size(), currentUserId);
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
            map.add("token", token);
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
        updatedFields.put("email", user.getEmail());
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
                .append("/realms/")
                .append(keycloakRealms)
                .append("/protocol/openid-connect/auth")
                .append("?client_id=").append(URLEncoder.encode(keycloakClientId, StandardCharsets.UTF_8))
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .append("&response_type=code")
                .append("&scope=").append(URLEncoder.encode("openid profile email", StandardCharsets.UTF_8));
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
            form.add("token", token);
            form.add(CLIENT_ID, this.keycloakClientId);
            form.add(CLIENT_SECRET, this.keycloakClientSecrete);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/jwt"); // Request JWT claim in response
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

            String introspectUrl = keycloakUrl + "/realms/" + this.keycloakRealms + "/protocol/openid-connect/token/introspect";
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
            user.setLogin(username);
            user.setEmail(email != null ? email : "");
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            
            // Extract ALL roles from realm_access and resource_access
            List<String> roles = new ArrayList<>();
            
            // Log what we're looking for
            LOGGER.debug("Looking for roles in introspection response...");
            LOGGER.debug("Available keys in response: {}", body.keySet());
            
            // 1. Extract realm-level roles
            Object realmAccess = body.get("realm_access");
            LOGGER.debug("realm_access: {}", realmAccess);
            if (realmAccess instanceof Map) {
                Object rolesObj = ((Map<?, ?>) realmAccess).get("roles");
                LOGGER.debug("realm_access.roles: {}", rolesObj);
                if (rolesObj instanceof List) {
                    for (Object role : (List<?>) rolesObj) {
                        if (role instanceof String s) {
                            roles.add(AuthoritiesConstants.ROLE_PREFIX + s);
                        }
                    }
                }
            }
            
            // 2. Extract ALL client-specific roles from resource_access
            Object resourceAccess = body.get("resource_access");
            LOGGER.debug("resource_access: {}", resourceAccess);
            if (resourceAccess instanceof Map) {
                Map<?, ?> resourceAccessMap = (Map<?, ?>) resourceAccess;
                LOGGER.debug("Found {} client entries in resource_access", resourceAccessMap.size());
                
                for (Map.Entry<?, ?> entry : resourceAccessMap.entrySet()) {
                    String clientName = entry.getKey().toString();
                    Object clientAccess = entry.getValue();
                    LOGGER.debug("Processing client: {}", clientName);
                    
                    if (clientAccess instanceof Map) {
                        Object clientRoles = ((Map<?, ?>) clientAccess).get("roles");
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
            }
            
            // 3. Also check for roles directly in the response (some Keycloak versions put them here)
            Object directRoles = body.get("roles");
            LOGGER.debug("Direct roles: {}", directRoles);
            if (directRoles instanceof List) {
                for (Object role : (List<?>) directRoles) {
                    if (role instanceof String s) {
                        roles.add(AuthoritiesConstants.ROLE_PREFIX + s);
                    }
                }
            }
            
            LOGGER.debug("Final extracted roles: {}", roles);
            user.setRoles(roles);
            
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
        LOGGER.debug("  - Code: {}...{}", code.substring(0, Math.min(8, code.length())), 
            code.substring(Math.max(0, code.length() - 8)));
        LOGGER.debug("  - Redirect URI: {}", redirectUri);
        LOGGER.debug("  - Client ID: {}", keycloakClientId);
        LOGGER.debug("  - Client Secret: {}", keycloakClientSecrete != null ? "***" : "null");
        
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("scope", "openid profile email"); // Add scope for roles and user info
        form.add(CLIENT_ID, keycloakClientId);
        form.add(CLIENT_SECRET, keycloakClientSecrete);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        // Build the token exchange URL
        String tokenUrl = keycloakUrl + "/realms/" + this.keycloakRealms + "/protocol/openid-connect/token";
        LOGGER.debug("Making token exchange request to: {}", tokenUrl);
        LOGGER.debug("Request headers: {}", headers);
        LOGGER.debug("Request body parameters:");
        LOGGER.debug("  - grant_type: authorization_code");
        LOGGER.debug("  - code: {}...{}", code.substring(0, Math.min(8, code.length())), 
            code.substring(Math.max(0, code.length() - 8)));
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
            Object accessToken = body.get("access_token");
            
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
            map.add("grant_type", "client_credentials");
            map.add("scope", "openid profile email");
            map.add(CLIENT_ID, username); // Use username as client_id for admin client
            map.add(CLIENT_SECRET, password); // Use password as client_secret for admin client

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // Build the full URL for admin client authentication
            String tokenUrl = keycloakUrl + REALMS + this.keycloakRealms + "/protocol/openid-connect/token";
            LOGGER.debug("Making admin client authentication request to: {}", tokenUrl);
            LOGGER.debug("Request headers: {}", headers);
            LOGGER.debug("Request body parameters: grant_type={}, scope={}, client_id={}, client_secret={}", 
                "client_credentials", "openid profile email", username, "***");

            Map<String, String> response = restTemplate.postForObject(tokenUrl, request, HashMap.class);
            
            if (response == null) {
                LOGGER.warn("Admin client authentication response is null");
                return null;
            }
            
            String accessToken = response.getOrDefault("access_token", "");
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
                keycloakUrl + REALMS + this.keycloakRealms + "/protocol/openid-connect/token",
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

    private void setRoleToUser(User user, Map map) {
        user.setRoles(new ArrayList<>());
        Map<String, Object> resourceAccess = (HashMap) map.get("resource_access");
        for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
            List<Object> roles = (List<Object>) ((HashMap) entry.getValue()).get("roles");
            for (Object role : roles) {
                user.getRoles().add(AuthoritiesConstants.ROLE_PREFIX + role.toString());
            }
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
}
