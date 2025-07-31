package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserInformationUpdateRequest;
import org.springframework.cache.annotation.CacheEvict;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PropertyFileIdentityProvider implements IdentityProvider {

	private final Map<String, User> users = new HashMap<>();
	private final Map<String, String> passwords = new HashMap<>();
	private final Map<String, User> authorisationTokens = new HashMap<>();
	private final FileSource usersFileSource;
	private final FileSource userGroupFileSource;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	PropertyFileIdentityProvider(String fileDirectory) throws IOException {
		File directory = new File(fileDirectory);
		if (!directory.exists()) {
			logger.error("File directory does not exist: '{}'", directory.getAbsolutePath());
			System.exit(1);
		}
		if (!directory.isDirectory()) {
			logger.error("File directory is not a directory: '{}'", directory.getAbsolutePath());
			System.exit(1);
		}
		usersFileSource = new FileSource(directory, "users.txt");
		userGroupFileSource = new FileSource(directory, "user-groups.txt");
		readFiles();
	}

	@Override
	public String authenticate(String username, String password) {
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			return null;
		}
		readFilesIfChanged();

		if (password.equals(passwords.get(username))) {
			User user = users.get(username);
			if (user != null) {
				synchronized (authorisationTokens) {
					for (Map.Entry<String, User> entry : authorisationTokens.entrySet()) {
						// If already authenticated, reuse existing token
						if (entry.getValue().equals(user)) {
							return entry.getKey();
						}
					}
					// No existing token found. Create new.
					String token = UUID.randomUUID().toString();
					authorisationTokens.put(token, user);
					return token;
				}
			}
		}
		return null;
	}

	@Override
	public User getUser(String username) {
		readFilesIfChanged();

		return users.get(username);
	}

	@Override
	public User getUserByToken(String token) {
		readFilesIfChanged();

		synchronized (authorisationTokens) {
			return authorisationTokens.get(token);
		}
	}

	@Override
	public List<String> getUserRoles(String username) {
		readFilesIfChanged();

		if (username == null || username.isEmpty()) {
			return Collections.emptyList();
		}

		User user = users.get(username);
		if (user != null) {
			return user.getRoles();
		}
		return List.of();
	}

	@Override
	public List<User> searchUsersByGroup(String groupName, String username, int maxResults, int startAt) {
		return users.values().stream()
				.filter(user -> (username == null || username.equals(user.getLogin())) && user.getRoles().contains(groupName))
				.map(User::publicClone)
				.toList();
	}

	@Override
	@CacheEvict(value = "accountCache", key = "#token")
	public boolean invalidateToken(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}

		synchronized (authorisationTokens) {
			authorisationTokens.remove(token);
		}
		return true;
	}

	@Override
	@CacheEvict(value = "accountCache", key = "#token")
	public User updateUser(User user, UserInformationUpdateRequest request, String token) {
		throw new UnsupportedOperationException("Password reset is not supported via API.");
	}

	@Override
	public void resetUserPassword(String username, String newPassword) {
		throw new UnsupportedOperationException("Password reset is not supported via API.");
	}

	public void readFilesIfChanged() {
		try {
			if (usersFileSource.hasChanged() || userGroupFileSource.hasChanged()) {
				readFiles();
			}
		} catch (IOException e) {
			throw new RuntimeServiceException("Failed to read user or group files.", e);
		}
	}

	public void readFiles() throws IOException {
		logger.info("Loading users and groups from files.");
		Properties userProperties = usersFileSource.readProperties();
		for (Map.Entry<Object, Object> entry : userProperties.entrySet()) {
			User user = new User();
			String username = (String) entry.getKey();
			user.setLogin(username);
			user.setActive(true);
			users.put(username, user);
			passwords.put(username, (String) entry.getValue());
		}
		Properties userGroupProperties = userGroupFileSource.readProperties();
		for (Map.Entry<Object, Object> entry : userGroupProperties.entrySet()) {
			String username = entry.getKey().toString();
			String groupList = (String) entry.getValue();
			List<String> groups = Arrays.stream(groupList.split(",")).map(String::trim).toList();
			User user = users.get(username);
			if (user == null) {
				throw new IllegalArgumentException(("User '%s' is defined in user-groups file " +
						"but not found in users file.").formatted(username));
			}
			user.setRoles(groups);
		}
		synchronized (authorisationTokens) {
			for (Map.Entry<String, User> entry : authorisationTokens.entrySet()) {
				entry.setValue(users.get(entry.getValue().getLogin()));
			}
		}
	}

}
