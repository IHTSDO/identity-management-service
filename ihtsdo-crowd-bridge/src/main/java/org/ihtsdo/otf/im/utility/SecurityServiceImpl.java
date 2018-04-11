package org.ihtsdo.otf.im.utility;

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
public final class SecurityServiceImpl implements SecurityService {

	/**
	 * Get the login of the current user.
	 */
	public String getCurrentLogin() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		UserDetails springSecurityUser = null;
		String userName = null;
		if (authentication != null) {
			if (authentication.getPrincipal() instanceof UserDetails) {
				springSecurityUser = (UserDetails) authentication.getPrincipal();
				userName = springSecurityUser.getUsername();
			} else if (authentication.getPrincipal() instanceof String) {
				userName = (String) authentication.getPrincipal();
			}
		}
		return userName;
	}

	/**
	 * Check if a user is authenticated.
	 * 
	 * @return true if the user is authenticated, false otherwise
	 */
	public boolean isAuthenticated() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Collection<? extends GrantedAuthority> authorities = securityContext.getAuthentication().getAuthorities();
		if (authorities != null) {
			for (GrantedAuthority authority : authorities) {
				if (authority.getAuthority().equals("ROLE_ANONYMOUS")) {
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * If the current user has a specific security role.
	 */
	public boolean isUserInRole(String role) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication != null) {
			if (authentication.getPrincipal() instanceof UserDetails) {
				UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
				return springSecurityUser.getAuthorities().contains(new SimpleGrantedAuthority(role));
			}
		}
		return false;
	}

	/**
	 * @return
	 * @throws AccessDeniedException
	 */
	public IHTSDOUser getCurrentUserDetails() throws AccessDeniedException {
		
		IHTSDOUser user =  IHTSDOUser.getInstance(SecurityContextHolder.getContext().getAuthentication());
		return user;
		
	}

}
