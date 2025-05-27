package org.snomed.ims.service;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.domain.crowd.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class CrowdRestClient implements IdentityProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CrowdRestClient.class);

	private final RestTemplate restTemplate;

	public CrowdRestClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Return token if authentication is successful; otherwise, return null.
	 *
	 * @param username Username to attempt authentication with
	 * @param password Password to attempt authentication with
	 * @return Token if authentication is successful; otherwise, return null.
	 */
	@Override
	public String authenticate(String username, String password) {
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			return null;
		}

		try {
			Session session = restTemplate.postForObject("/session", Map.of(USERNAME, username, PASSWORD, password), Session.class);
			if (session == null) {
				return null;
			}

			return session.getToken();
		} catch (Exception e) {
			LOGGER.error("6ab2da39-aae9-420e-88b5-0412628fbb96 Failed to authenticate", e);
			return null;
		}
	}

	/**
	 * Return user if found by username; otherwise return null.
	 *
	 * @param username Username to match against User.
	 * @return User if found by username; otherwise return null.
	 */
	@Override
	public User getUser(String username) {
		if (username == null || username.isEmpty()) {
			return null;
		}

		try {
			return restTemplate.getForObject("/user?username={username}", User.class, Map.of(USERNAME, username));
		} catch (Exception e) {
			LOGGER.error("806fd9be-4c61-41aa-87dd-e3ab0ef1250f Failed to get user", e);
			return null;
		}
	}

	/**
	 * Return user if found by token; otherwise return null.
	 *
	 * @param token Token to match against User.
	 * @return User if found by token; otherwise return null.
	 */
	@Override
	@Cacheable(value = "accountCache", key = "#token", unless = "#result == null")
	public User getUserByToken(String token) {
		if (token == null || token.isEmpty()) {
			return null;
		}

		try {
			Session session = restTemplate.getForObject("/session/{token}", Session.class, Map.of("token", token));
			if (session == null) {
				return null;
			}

			User user = session.getUser();
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

	/**
	 * Return roles for user if username found; otherwise return empty.
	 *
	 * @param username Username to match against User.
	 * @return Roles for user if username found; otherwise return empty.
	 */
	@Override
	public List<String> getUserRoles(String username) {
		if (username == null || username.isEmpty()) {
			return Collections.emptyList();
		}

		try {
			GroupsCollection groups = restTemplate.getForObject("/user/group/direct?username={username}", GroupsCollection.class, Map.of(USERNAME, username));
			if (groups == null) {
				return Collections.emptyList();
			}

			List<String> roles = new ArrayList<>();
			List<String> groupNames = groups.getGroupNames();
			for (String groupName : groupNames) {
				roles.add(AuthoritiesConstants.ROLE_PREFIX + groupName);
			}

			return roles;
		} catch (Exception e) {
			LOGGER.error("7b0fe742-a69a-4a34-8cdc-ce86a312c95a Failed to get user's roles", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Return users in group. If group or username not found, return empty.
	 *
	 * @param groupName  Group name to match against Users.
	 * @param username   Username to match against individual User.
	 * @param maxResults Size of page request.
	 * @param startAt    Offset of page request.
	 * @return Users in group. If group or username not found, return empty.
	 */
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

	/**
	 * Return whether inactivating token has been successful.
	 *
	 * @param token Token to invalidate.
	 * @return Whether inactivating token has been successful.
	 */
	@Override
	@CacheEvict(value = "accountCache", key = "#token")
	public boolean invalidateToken(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(List.of(MediaType.APPLICATION_JSON));

			restTemplate.exchange("/session/" + token, HttpMethod.DELETE, new HttpEntity<>(headers), Object.class);
			return true;
		} catch (Exception e) {
			LOGGER.error("11153184-02ce-425a-94bf-8540d014f6d6 Failed to invalidate token", e);
			return false;
		}
	}

	@Override
	@CacheEvict(value = "accountCache", key = "#token")
	public User updateUser(User user, UserInformationUpdateRequest request, String token) {
        Map<String, String> updatedFields = new HashMap<>();
        updatedFields.put(NAME, user.getLogin());
        updatedFields.put(EMAIL, user.getEmail());

        if (request.firstName() != null) {
            updatedFields.put(FIRST_NAME, request.firstName());
        }
        if (request.lastName() != null) {
            updatedFields.put(LAST_NAME, request.lastName());
        }
        if (request.displayName() != null) {
            updatedFields.put(DISPLAY_NAME, request.displayName());
        }
        restTemplate.put("/user?username={username}", updatedFields, Map.of(USERNAME, user.getLogin()));
        return this.getUser(user.getLogin());
    }

	@Override
	public void resetUserPassword(String username, String newPassword) {
		Map<String, Object> params = new HashMap<>();
		params.put(USERNAME, username);

		Map<String, Object> body = new HashMap<>();
		body.put("value", newPassword);
		restTemplate.put("/user/password?username={username}", body, params);
	}
}