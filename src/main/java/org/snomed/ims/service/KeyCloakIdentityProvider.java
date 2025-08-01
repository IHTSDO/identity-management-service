package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.domain.crowd.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class KeyCloakIdentityProvider implements IdentityProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyCloakIdentityProvider.class);
    private static final String ADMIN_REALMS = "/admin/realms/";
    private static final String REALMS = "/realms/";
    private static final String USERS = "/users/";

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
            map.add("client_id", this.keycloakClientId);
            map.add("client_secret", this.keycloakClientSecrete);
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

            Set<User> response = restTemplate.getForObject("/admin/realms/ " + this.keycloakRealms + "/user?exact=true&username={username}", HashSet.class, Map.of(USERNAME, username));
            if (response == null || response.isEmpty()) {
                return null;
            }
            return response.iterator().next();
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
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            User user = restTemplate.getForObject(REALMS + this.keycloakRealms + "/protocol/openid-connect/userinfo", User.class, entity);
            if (user == null) {
                return null;
            }

            user.setRoles(getUserRoles(user.getLogin()));
            return user;
        } catch (Exception e) {
            LOGGER.error("0cdbce8a-e4cc-411f-bc71-64659ad027b3 Failed to get user by token", e);
            return null;
        }
    }

    @Override
    public List<String> getUserRoles(String username) {
        if (username == null || username.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String adminToken = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            Set<User> userResponse = restTemplate.getForObject("/admin/realms/ " + this.keycloakRealms + "/user?exact=true&username={username}", HashSet.class, Map.of(USERNAME, username));
            if (userResponse.isEmpty()) {
                return Collections.emptyList();
            }
            User user = userResponse.iterator().next();


            ResponseEntity<GroupsCollection> groupResponse = restTemplate.exchange(
                    ADMIN_REALMS + this.keycloakRealms + USERS + user.getId() + "/role-mappings/realm/available",
                    HttpMethod.GET,
                    requestEntity,
                    GroupsCollection.class
            );

            if (groupResponse.getBody() == null) {
                return Collections.emptyList();
            }

            List<String> roles = new ArrayList<>();
            List<String> groupNames = groupResponse.getBody().getGroupNames();
            for (String groupName : groupNames) {
                roles.add(AuthoritiesConstants.ROLE_PREFIX + groupName);
            }

            return roles;
        } catch (Exception e) {
            LOGGER.error("97a939d9-6ac6-4db8-954e-3c4ea425d95e Failed to get user's roles", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<User> searchUsersByGroup(String groupName, String username, int maxResults, int startAt) {
        if (groupName == null || groupName.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("groupname", groupName);
        params.put("maxResults", maxResults);
        params.put("startIndex", startAt);
        if (StringUtils.hasLength(username)) {
            params.put(USERNAME, username);
        }

        try {
            UsersCollection response = restTemplate.getForObject("/group/user/direct?groupname={groupname}&max-results={maxResults}&start-index={startIndex}" + (StringUtils.hasLength(username) ? "&username={username}" : ""), UsersCollection.class, params);
            if (response == null) {
                return Collections.emptyList();
            }

            if (response.hasOneUser()) {
                User user = new User();
                user.setLogin(response.getName());
                response.addUser(user);
            }

            List<User> users = new ArrayList<>();
            for (User u : response.getUsers()) {
                User user = getUser(u.getLogin());
                if (user == null) {
                    continue;
                }

                user.setEmail(null);
                users.add(user);
            }

            return users;
        } catch (Exception e) {
            LOGGER.error("7000bbc0-9443-43b5-809b-3f135fdb46ba Failed to get users by group name", e);
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
            map.add("client_id", this.keycloakClientId);
            map.add("client_secret", this.keycloakClientSecrete);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            restTemplate.postForObject(REALMS + this.keycloakRealms + "/protocol/openid-connect/revoke", request, Void.class);
            return true;
        } catch (Exception e) {
            LOGGER.error("11153184-02ce-425a-94bf-8540d014f6d6 Failed to invalidate token", e);
            return false;
        }
    }

    @Override
    @CacheEvict(value = "accountCache", key = "#token")
    public User updateUser(User user, UserInformationUpdateRequest request, String token) {
        String adminToken = getAdminToken();
        MultiValueMap<String, String> updatedFields = new LinkedMultiValueMap<>();
        updatedFields.add(NAME, user.getLogin());
        updatedFields.add(EMAIL, user.getEmail());

        if (request.firstName() != null) {
            updatedFields.add(FIRST_NAME, request.firstName());
        }
        if (request.lastName() != null) {
            updatedFields.add(LAST_NAME, request.lastName());
        }
        if (request.displayName() != null) {
            updatedFields.add(DISPLAY_NAME, request.displayName());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(adminToken);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(updatedFields, headers);

        restTemplate.exchange(
                ADMIN_REALMS + this.keycloakRealms + USERS + user.getId(),
                HttpMethod.PUT,
                entity,
                Void.class
        );
        return this.getUser(user.getLogin());
    }

    @Override
    public void resetUserPassword(User user, String newPassword) {
        String adminToken = getAdminToken();
        MultiValueMap<String, String> updatedFields = new LinkedMultiValueMap<>();
        updatedFields.add("type", "password");
        updatedFields.add("temporary", "false");
        updatedFields.add("value", newPassword);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(adminToken);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(updatedFields, headers);

        restTemplate.exchange(
                ADMIN_REALMS + this.keycloakRealms + USERS + user.getId() + "/reset-password",
                HttpMethod.PUT,
                entity,
                Void.class);
    }

    private String getAdminToken() {
        return authenticate(this.keycloakAdminUsername, this.keycloakAdminPassword);
    }
}
