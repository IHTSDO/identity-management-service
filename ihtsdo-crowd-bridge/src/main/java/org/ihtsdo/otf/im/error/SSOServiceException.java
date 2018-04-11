package org.ihtsdo.otf.im.error;

/**
 * This exception is to wrap crowd service exception which can be categorized  as system exception
 */
public class SSOServiceException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SSOServiceException(String message) {
        super(message);
    }

    public SSOServiceException(String message, Throwable t) {
        super(message, t);
    }
}
