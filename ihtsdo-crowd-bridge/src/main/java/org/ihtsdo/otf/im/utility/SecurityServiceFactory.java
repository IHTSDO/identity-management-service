package org.ihtsdo.otf.im.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityServiceFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceFactory.class);

	private SecurityService onlineImplementation;

	private SecurityService stubImplementation;

	public SecurityService getSecurityService(boolean stub) {
		if (stub) {
			LOGGER.info("Using Stubbed IMS Security Service");
			return stubImplementation;
		} else {
			LOGGER.info("Using Online IMS Security Service");
			return onlineImplementation;
		}
	}

	public void setOnlineImplementation(SecurityService onlineImplementation) {
		this.onlineImplementation = onlineImplementation;
	}

	public void setStubImplementation(SecurityService stubImplementation) {
		this.stubImplementation = stubImplementation;
	}

}
