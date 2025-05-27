package org.snomed.ims.service;

import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserInformationUpdateRequest;

import java.util.List;

public interface IdentityProvider {

	String USERNAME = "username";
	String PASSWORD = "password";
	String NAME = "name";
	String FIRST_NAME = "first-name";
	String LAST_NAME = "last-name";
	String DISPLAY_NAME = "display-name";
	String EMAIL = "email";

	String authenticate(String username, String password);

	User getUser(String username);

	User getUserByToken(String token);

	List<String> getUserRoles(String username);

	List<User> searchUsersByGroup(String groupName, String username, int maxResults, int startAt);

	boolean invalidateToken(String token);

	User updateUser(User user, UserInformationUpdateRequest request, String token);

	void resetUserPassword(String username, String newPassword);
}
