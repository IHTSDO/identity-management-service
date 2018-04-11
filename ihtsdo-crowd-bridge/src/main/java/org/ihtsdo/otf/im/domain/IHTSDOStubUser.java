/**
 * 
 */
package org.ihtsdo.otf.im.domain;

import org.springframework.security.core.userdetails.UserDetails;


/**IHTSDO specific implementation of {@link UserDetails} 
 * It can be used to get detail of user across IHTSDO tools.
 * It provide user details as well as their roles details by using already authenticated principal
 *
 */
public class IHTSDOStubUser extends IHTSDOUser {

	private static final long serialVersionUID = 1L;

	public IHTSDOStubUser(String username) {
		super(username);
	}
}
