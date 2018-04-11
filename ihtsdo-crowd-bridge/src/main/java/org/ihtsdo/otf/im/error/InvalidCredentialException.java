package org.ihtsdo.otf.im.error;

import org.springframework.security.core.AuthenticationException;

/**
 * This exception is throw in case provided password/username does not follow system defined rules.
 */
public class InvalidCredentialException extends AuthenticationException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidCredentialException(String message) {
        super(message);
    }

    public InvalidCredentialException(String message, Throwable t) {
        super(message, t);
    }
}
