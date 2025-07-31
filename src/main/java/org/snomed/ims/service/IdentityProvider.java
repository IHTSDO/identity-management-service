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

	/**
	 * Return token if authentication is successful; otherwise, return null.
	 *
	 * @param username Username to attempt authentication with
	 * @param password Password to attempt authentication with
	 * @return Token if authentication is successful; otherwise, return null.
	 */
	String authenticate(String username, String password);

	/**
	 * Return user if found by username; otherwise return null.
	 *
	 * @param username Username to match against User.
	 * @return User if found by username; otherwise return null.
	 */
	User getUser(String username);

	/**
	 * Return user if found by token; otherwise return null.
	 *
	 * @param token Token to match against User.
	 * @return User if found by token; otherwise return null.
	 */
	User getUserByToken(String token);

	/**
	 * Return roles for user if username found; otherwise return empty.
	 *
	 * @param username Username to match against User.
	 * @return Roles for user if username found; otherwise return empty.
	 */
	List<String> getUserRoles(String username);

	/**
	 * Return users in group. If group or username not found, return empty.
	 *
	 * @param groupName  Group name to match against Users.
	 * @param username   Username to match against individual User.
	 * @param maxResults Size of page request.
	 * @param startAt    Offset of page request.
	 * @return Users in group. If group or username not found, return empty.
	 */
	List<User> searchUsersByGroup(String groupName, String username, int maxResults, int startAt);

	/**
	 * Return whether inactivating token has been successful.
	 *
	 * @param token Token to invalidate.
	 * @return Whether inactivating token has been successful.
	 */
	boolean invalidateToken(String token);

	User updateUser(User user, UserInformationUpdateRequest request, String token);

	void resetUserPassword(String username, String newPassword);
}
