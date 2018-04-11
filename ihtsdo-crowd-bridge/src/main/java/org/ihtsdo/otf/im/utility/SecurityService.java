package org.ihtsdo.otf.im.utility;

import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.springframework.security.access.AccessDeniedException;

/**
 * The implementations of this interface are intended to replace SecurityUtils which - because of it's static nature, don't lend themselves
 * to run time substitution (eg choosing the stubbed version for local development)
 */
public interface SecurityService {

	/**
	 * Get the login of the current user.
	 */
	public String getCurrentLogin();

	/**
	 * Check if a user is authenticated.
	 * 
	 * @return true if the user is authenticated, false otherwise
	 */
	public boolean isAuthenticated();

	/**
	 * If the current user has a specific security role.
	 */
	public boolean isUserInRole(String role);

	/**
	 * @return
	 * @throws AccessDeniedException
	 */
	public IHTSDOUser getCurrentUserDetails() throws AccessDeniedException;

}
