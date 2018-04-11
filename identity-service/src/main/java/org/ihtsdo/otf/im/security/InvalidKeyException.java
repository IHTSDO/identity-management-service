package org.ihtsdo.otf.im.security;

import org.springframework.security.core.AuthenticationException;

/**
 * This exception is throw in case of a user trying to use an invalid/expired  key to activate/ or password reset.
 */
public class InvalidKeyException extends AuthenticationException {

	public InvalidKeyException(String message) {
		super(message);
	}

	public InvalidKeyException(String message, Throwable t) {
		super(message, t);
	}
}
