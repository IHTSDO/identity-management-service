/**
 * 
 */
package org.ihtsdo.otf.im.domain;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;

/**IHTSDO specific implementation of {@link UserDetails} 
 * It can be used to get detail of user across IHTSDO tools.
 * It provide user details as well as their roles details by using already authenticated principal
 *
 */
public class IHTSDOUser implements UserDetails {
		
    private String userName;
    private String firstName;
    private String lastName;
    private boolean active;
    private String emailAddress;
    private String displayName;
    private Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private String languageKey;
    private String activationKey;
    
    private IHTSDOUser( String userName,  String firstName,
    		String lastName,  boolean active,
    		String emailAddress,
    		String displayName, Collection<GrantedAuthority> authorities, 
    		boolean accountNonExpired,
    		boolean accountNonLocked,
    		boolean credentialsNonExpired, String langKey)
    {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.languageKey = langKey;
    }

	protected IHTSDOUser(String username) {
		this.userName = username;
	}

    
    public String getFirstName()
    {
        return this.firstName;
    }

    public String getLastName()
    {
        return this.lastName;
    }

    public String getEmailAddress()
    {
        return this.emailAddress;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	/**
	 * @return
	 */
	public static IHTSDOUser getInstance(Authentication auth) {
		
		if (auth == null) {
			
			throw new AccessDeniedException("Invalid user principal");

		}
		
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
		
		Collection<GrantedAuthority> userAuthorities = new ArrayList<GrantedAuthority>();
		
		userAuthorities.addAll(authorities);
		
		if (auth.getPrincipal() instanceof CrowdUserDetails) {
			
			CrowdUserDetails ud = (CrowdUserDetails)auth.getPrincipal();
			IHTSDOUser user = new IHTSDOUser(ud.getUsername(), ud.getFirstName(), 
					ud.getLastName(), ud.isEnabled(), 
					ud.getEmail(), ud.getFullName(), 
					userAuthorities, ud.isAccountNonExpired(), ud.isAccountNonLocked(), ud.isCredentialsNonExpired(), ud.getAttribute("langKey"));
			user.setActivationKey(ud.getAttribute("activationKey"));
			return user;
		} else {
			
			throw new AccessDeniedException("Invalid user principal");
		}
	}
	
	/**
	 * @param an instance of {@link CrowdUserDetails}
	 * @return an instance of {@link IHTSDOUser}
	 */
	public static IHTSDOUser getInstance(CrowdUserDetails ud) {
		
		if (ud != null) {

			Collection<GrantedAuthority> userAuthorities = new ArrayList<GrantedAuthority>();
			userAuthorities.addAll(ud.getAuthorities());
			IHTSDOUser user = new IHTSDOUser(ud.getUsername(), ud.getFirstName(), 
					ud.getLastName(), ud.isEnabled(), 
					ud.getEmail(), ud.getFullName(), 
					userAuthorities, ud.isAccountNonExpired(), 
					ud.isAccountNonLocked(), 
					ud.isCredentialsNonExpired(), ud.getAttribute("langKey"));
			user.setActivationKey(ud.getAttribute("activationKey"));
			return user;
		} else {
			
			return new IHTSDOUser("gues", "Guest", null, true, null, "Guest", new ArrayList<GrantedAuthority>(), true, true, true, "en");
		}
	
	}

	/**
	 * @return the authorities
	 */
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getPassword()
	 */
	@Override
	public String getPassword() {
		//never return a password let user deal with it by reset/forgot password etc
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getUsername()
	 */
	@Override
	public String getUsername() {
		
		return this.userName;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
	 */
	@Override
	public boolean isAccountNonExpired() {
		
		return this.accountNonExpired;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
	 */
	@Override
	public boolean isAccountNonLocked() {
		
		return this.accountNonLocked;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		
		return this.credentialsNonExpired;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		
		return this.active;
	}
	
	public String getLangKey() {
		
		return this.languageKey;
	}


	/**
	 * @return the activationKey
	 */
	public String getActivationKey() {
		return activationKey;
	}


	/**
	 * @param activationKey the activationKey to set
	 */
	private void setActivationKey(String activationKey) {
		this.activationKey = activationKey;
	}
}
