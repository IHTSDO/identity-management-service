package org.snomed.ims.middle;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

	private AuthoritiesConstants() {
	}

	public static final String IMS_ADMIN = "ROLE_ims-administrators";

	public static final String IHTDO_OPS_ADMIN = "ROLE_ihtsdo-ops-admin";

	public static final String USER = "ROLE_ihtsdo-users";

	public static final String ANONYMOUS = "ROLE_ANONYMOUS";

	public static final String EXTERNAL_USER = "ROLE_external-users";

	public static final String ROLE_PREFIX = "ROLE_";

}
