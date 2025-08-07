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

    private final String keycloakRealms;

    private final String keycloakClientId;

    private final String keycloakClientSecrete;

    private final String keycloakAdminUsername;

    private final String keycloakAdminPassword;

    public KeyCloakIdentityProvider(RestTemplate restTemplate, String keycloakRealms, String keycloakClientId, String keycloakClientSecrete, String keycloakAdminUsername, String keycloakAdminPassword) {
        this.restTemplate = restTemplate;
        this.keycloakRealms = keycloakRealms;
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecrete = keycloakClientSecrete;
        this.keycloakAdminUsername = keycloakAdminUsername;
        this.keycloakAdminPassword = keycloakAdminPassword;
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
            return Collections.emptyList();
        }
        String adminToken = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List<KeyCloakGroup>> groupResponse = restTemplate.exchange(
                    ADMIN_REALMS + this.keycloakRealms + USERS + currentUserId + "/groups",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<KeyCloakGroup> keyCloakGroups = groupResponse.getBody();
            if (CollectionUtils.isEmpty(keyCloakGroups)) {
                return Collections.emptyList();
            }
            List<User> users = getUsersForGroup(groupName, username, keyCloakGroups, requestEntity);
            if (startAt >= 0 && startAt < users.size()) {
                int toIndex = Math.min(startAt + maxResults, users.size());
                return users.subList(startAt, toIndex);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            LOGGER.error("620cdd4c-f4c4-4105-8ebd-96b1925df746 Failed to get users by group name", e);
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

    private String getAdminToken() {
        return authenticate(this.keycloakAdminUsername, this.keycloakAdminPassword);
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
                    ResponseEntity<List<KeyCloakUser>> userResponse = restTemplate.exchange(
                            ADMIN_REALMS + this.keycloakRealms + "/groups/" + group.getId() + "/members?max=-1",
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<>() {
                            }
                    );
                    List<KeyCloakUser> keyCloakUsers = userResponse.getBody();
                    if (CollectionUtils.isEmpty(keyCloakUsers)) {
                        return Stream.empty();
                    }
                    return keyCloakUsers.stream()
                            .filter(KeyCloakUser::isEnabled)
                            .filter(u -> !StringUtils.hasLength(username) || u.getUsername().contains(username))
                            .map(u -> {
                                User user = toUser(u);
                                user.setEmail(null);
                                return user;
                            });
                }).distinct().toList();
    }
}
