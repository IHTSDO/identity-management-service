package org.ihtsdo.otf.im.utility;

import org.ihtsdo.otf.im.domain.IHTSDOStubUser;
import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Utility class for Spring Security.
 */
public final class SecurityServiceStub implements SecurityService {

	private boolean isAuthenticated = true;
	private List<String> roles;
	private String currentLogin = "testUser";

	/**
	 * Get the login of the current user.
	 */
	public String getCurrentLogin() {
		return currentLogin;
	}

	/**
	 * Check if a user is authenticated.
	 * 
	 * @return true if the user is authenticated, false otherwise
	 */
	public boolean isAuthenticated() {
		return isAuthenticated;
	}


	/**
	 * If the current user has a specific security role.
	 */
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

	/**
	 * @return
	 * @throws AccessDeniedException
	 */
	public IHTSDOUser getCurrentUserDetails() throws AccessDeniedException {
		
		IHTSDOUser user = new IHTSDOStubUser(this.currentLogin);
		return user;
		
	}

	public void setAuthenticated(boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public void setCurrentLogin(String currentLogin) {
		this.currentLogin = currentLogin;
	}
}
