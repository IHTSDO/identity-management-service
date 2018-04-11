package org.ihtsdo.otf.im.error;

import org.springframework.security.core.AuthenticationException;

/**
 * This exception is throw in case an user with given login does not exist in the system
 */
public class UserNotFoundException extends AuthenticationException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable t) {
        super(message, t);
    }
}
