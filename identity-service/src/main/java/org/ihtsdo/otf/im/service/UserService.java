package org.ihtsdo.otf.im.service;

import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.UserWithAttributes;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.CrowdClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.ihtsdo.otf.im.domain.IHTSDOToolsAuthority;
import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.domain.WritableUser;
import org.ihtsdo.otf.im.error.SSOServiceException;
import org.ihtsdo.otf.im.security.InvalidKeyException;
import org.ihtsdo.otf.im.security.SecurityUtils;
import org.ihtsdo.otf.im.sso.service.IHTSDOUserDetailsService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

	private static final int ALL_RESULTS = -1;

	private static final String ACTIVATION_KEY = "activationKey";
	private static final String LANG_KEY = "langKey";
	private static final String ACTIVATED = "activated";
	private static final String PASSWORD_RESET_TOKEN = "passwordResetToken";

	private static final String TOKEN_KEY = ":Token:";

	private static final String LOGIN_KEY = "Login:";

	private static final String DEFAULT_GROUP = "external-users";

	private final Logger log = LoggerFactory.getLogger(UserService.class);


	@Inject
	private IHTSDOUserDetailsService userRepository;

	@Inject
	private CrowdClient client;


	public IHTSDOUser activateRegistration(String key) {

		log.debug("Activating user for activation key {}", key);

		String decodedKey = !StringUtils.isEmpty(key) ? new String(Base64.decodeBase64(key.getBytes())) : key;

		log.debug("Decoded activation key {}", decodedKey);

		if (!StringUtils.isEmpty(decodedKey)
				&& decodedKey.contains(TOKEN_KEY) && decodedKey.contains(LOGIN_KEY)) {

			String userName = decodedKey.substring(decodedKey.indexOf(LOGIN_KEY)
					+ LOGIN_KEY.length(), decodedKey.indexOf(TOKEN_KEY));
			WritableUser user = getUserWithAttribute(userName);

			if (user != null && key.equalsIgnoreCase(user.getActivationKey())) {

				user.setActivated(true);
				user.setActivationKey(ACTIVATED);
				try {

					client.updateUser(user);

					//update activation key
					Map<String, Set<String>> attributes = new HashMap<>();
					Set<String> values = new HashSet<>();
					values.add(ACTIVATED);

					attributes.put(ACTIVATION_KEY, values);
					client.storeUserAttributes(user.getUserName(), attributes);

				} catch (UserNotFoundException | InvalidUserException e) {

					log.error("Exception during crowd API call", e);
					throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

				} catch (OperationFailedException | InvalidAuthenticationException
						| ApplicationPermissionException e) {

					log.error("Exception during crowd API call", e);
					throw new SSOServiceException(e.getMessage());
				}
				log.debug("Activated user: {}", user);
				return userRepository.getUserByUserName(user.getUserName());

			} else {

				throw new InvalidKeyException("Invalid activation key supplied, please use resend activation key");

			}

		} else {

			throw new InvalidKeyException("Invalid activation key supplied, please use resend activation key");

		}
	}

	public WritableUser validatePasswordResetKey(String key, String password) {

		log.debug("Validatte password reset key {}", key);
		if (StringUtils.isEmpty(key) | StringUtils.isEmpty(password)) {

			throw new InvalidKeyException("Invalid password reset link");

		}
		String decodedKey = new String(Base64.decodeBase64(key.getBytes()));

		log.debug("Decoded password reset key {}", decodedKey);

		String[] delimitedKeys = StringUtils.tokenizeToStringArray(decodedKey, "|");

		if (delimitedKeys != null && delimitedKeys.length == 3) {

			String expireTime = delimitedKeys[0];
			long timeStamp = StringUtils.isEmpty(expireTime) ? 0 : new Long(delimitedKeys[0]);

			if (timeStamp == 0 || new DateTime(timeStamp).isAfterNow()) {

				log.debug("Password reset link expired? {}", new DateTime(timeStamp).isAfterNow());

				throw new InvalidKeyException("Password reset link expired. please use forgot password feature to reset password.");

			} else {

				String userName = delimitedKeys[1];
				log.debug("Password reset user : {}", userName);

				WritableUser user = getUserWithAttribute(userName);

				String token = delimitedKeys[2];
				String storedToken = getStoredToken(user);
				log.debug("Password reset token received {} : stored {}",
						token, storedToken);

				if (user != null && !StringUtils.isEmpty(token) && token.equals(storedToken)) {

					PasswordCredential credentials = new PasswordCredential(password);

					log.debug("Changed password for User: {}", userName);
					try {

						client.updateUserCredential(userName, credentials.getCredential());

						log.debug("Removing PASSWORD_RESET_TOKEN for User: {}", userName);

						//then remove PASSWORD_RESET_TOKEN
						client.removeUserAttributes(userName, PASSWORD_RESET_TOKEN);


					} catch (UserNotFoundException | InvalidCredentialException e) {

						log.error("Exception during crowd API call", e);
						throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

					} catch (OperationFailedException | InvalidAuthenticationException
							| ApplicationPermissionException e) {

						log.error("Exception during crowd API call", e);
						throw new SSOServiceException(e.getMessage());
					}

					return user;

				}
			}
		}

		log.error("Invalid password reset key");

		throw new InvalidKeyException("Invalid password reset key, please use forgot password feature to reset password");


	}

	/**
	 * @param user
	 * @return
	 */
	private String getStoredToken(WritableUser user) {

		String key = user != null ? user.getPasswordResetKey() : "";
		if (StringUtils.isEmpty(key)) {

			return null;
		}
		String[] tokens = StringUtils.tokenizeToStringArray(new String(Base64.decodeBase64(key.getBytes())), "|");
		if (tokens != null && tokens.length == 3) {

			return tokens[2];
		}


		return null;
	}

	public WritableUser createUserInformation(String login, String password, String firstName, String lastName, String email,
											  String langKey) {
		WritableUser newUser = new WritableUser();
		newUser.setUserName(login);
		newUser.setFirstName(firstName);
		newUser.setLastName(lastName);
		newUser.setEmail(email);
		newUser.setActivated(false);
		newUser.setPassword(password);
		PasswordCredential credentials = new PasswordCredential(password);

		try {

			client.addUser(newUser, credentials);
			//updating as earlier sent credential is not being stored
			client.updateUserCredential(newUser.getUserName(), credentials.getCredential());

			//add extra attributes 
			Map<String, Set<String>> attributes = new HashMap<>();
			Set<String> values = new HashSet<>();
			values.add(StringUtils.isEmpty(langKey) ? "en" : langKey);

			attributes.put(LANG_KEY, values);

			values = new HashSet<>();
			String token = String.format("%s%s%s%s", LOGIN_KEY, login, TOKEN_KEY, UUID.randomUUID());
			values.add(Base64.encodeBase64URLSafeString(token.getBytes()));

			attributes.put(ACTIVATION_KEY, values);

			client.storeUserAttributes(newUser.getUserName(), attributes);

			//add default user group
			try {

				client.addUserToGroup(newUser.getUserName(), DEFAULT_GROUP);

			} catch (GroupNotFoundException e) {

				log.error("Default {} group membership already exist for user {}", DEFAULT_GROUP, newUser.getUserName());

			} catch (MembershipAlreadyExistsException e) {

				log.info("Error while adding user {} to default {} group", newUser.getUserName(), DEFAULT_GROUP);
			}

			WritableUser user = getUserWithAttribute(login);

			log.debug("Created Information for User: {}", user);

			return user;


		} catch (UserNotFoundException | InvalidUserException | InvalidCredentialException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		}


	}

	public void updateUserInformation(String firstName, String lastName, String email, String langKey) {


		WritableUser currentUser = getUserWithAttribute(SecurityUtils.getCurrentLogin());
		currentUser.setFirstName(firstName);
		currentUser.setLastName(lastName);
		currentUser.setEmail(email);

		//User coming from Jira etc does not have langugage key so update when user update information
		Map<String, Set<String>> attributes = new HashMap<>();
		Set<String> values = new HashSet<>();
		values.add(StringUtils.isEmpty(langKey) ? "en" : langKey);
		attributes.put(LANG_KEY, values);

		try {

			client.updateUser(currentUser);
			client.storeUserAttributes(SecurityUtils.getCurrentLogin(), attributes);

		} catch (UserNotFoundException | InvalidUserException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		}


		log.debug("Changed Information for User: {}", currentUser);
	}

	public void changePassword(String password) {
		try {
			PasswordCredential credentials = new PasswordCredential(password);

			log.debug("Changed password for User: {}", SecurityUtils.getCurrentLogin());
			client.updateUserCredential(SecurityUtils.getCurrentLogin(), credentials.getCredential());

		} catch (UserNotFoundException | InvalidCredentialException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());

		}

	}

	public IHTSDOUser getUserWithAuthorities() {

		return userRepository.getUserByUserName(SecurityUtils.getCurrentLogin());
	}

	private WritableUser getUserWithAttribute(String userName) {

		log.debug("Getting user details for User: {}", userName);


		try {

			UserWithAttributes ssoUser = client.getUserWithAttributes(userName);

			return getSSOUser(ssoUser);

		} catch (UserNotFoundException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		}
	}

	private WritableUser getSSOUser(UserWithAttributes ssoUser) {

		WritableUser user = new WritableUser();
		user.setActivated(ssoUser.isActive());
		user.setEmail(ssoUser.getEmailAddress());
		user.setFullName(ssoUser.getDisplayName());
		user.setFirstName(ssoUser.getFirstName());
		user.setLastName(ssoUser.getLastName());
		user.setUserName(ssoUser.getName());
		Set<String> attributeKeys = ssoUser.getKeys();
		for (String key : attributeKeys) {

			if (ACTIVATION_KEY.equals(key)) {

				String activationKey = ssoUser.getValue(key);
				user.setActivationKey(activationKey);

			}

			if (LANG_KEY.equals(key)) {

				String langKey = ssoUser.getValue(key);
				user.setLangKey(langKey);

			}

			if (PASSWORD_RESET_TOKEN.equals(key)) {

				String resetLink = ssoUser.getValue(key);
				user.setPasswordResetKey(resetLink);
			}
		}
		//get groups

		try {

			List<Group> subscribedGroups = client.getGroupsForUser(ssoUser.getName(), 0, ALL_RESULTS);
			for (Group group : subscribedGroups) {

				if (group.isActive()) {

					IHTSDOToolsAuthority ihtsdoToolsAuthority = new IHTSDOToolsAuthority(group.getName());
					ihtsdoToolsAuthority.setDescription(group.getDescription());

				}
			}

			//

		} catch (UserNotFoundException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		}
		log.debug("User is: {}", user);

		return user;


	}

	public WritableUser requestPasswordReset(String userName) {

		log.debug("requestPasswordReset for User: {}", userName);

		try {
			String token = getPasswordResetToken(userName);

			log.debug("token {} for User: {}", token, userName);


			//client.requestPasswordReset(userName); not doing crowd based password reset as it sends a reset email which redirect user to crowd ui instead of IM ui
			Map<String, Set<String>> attributes = new HashMap<>();
			Set<String> values = new HashSet<>();
			values.add(token);
			attributes.put(PASSWORD_RESET_TOKEN, values);

			client.storeUserAttributes(userName, attributes);

			UserWithAttributes ssoUser = client.getUserWithAttributes(userName);
			return getSSOUser(ssoUser);

		} catch (UserNotFoundException e) {

			log.error("Exception during crowd API call", e);
			throw new org.ihtsdo.otf.im.error.UserNotFoundException(e.getMessage());

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {

			log.error("Erro while generating reset token", e);
			throw new RuntimeException("Please check algorithm used in generating password reset token. "
					+ "Till then this process can not continue ", e.getCause());
		}

	}


	/**
	 * Generate a secure password reset token with format expiretime (milli seconds)|username|token
	 *
	 * @param userName
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private String getPasswordResetToken(String userName) throws NoSuchAlgorithmException {

		//SecureRandom sr = SecureRandom.getInstance("SHA1PRNG"); commenting as two slow
		//sr.setSeed(sr.generateSeed(20));//always seed explicitly
		String randomNum = RandomStringUtils.randomNumeric(20); //new Integer(sr.nextInt()).toString();


		DateTime dt = new DateTime();
		dt.plusHours(24);

		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		String token = Base64.encodeBase64URLSafeString(sha.digest(randomNum.getBytes()));

		return Base64.encodeBase64URLSafeString(String.format("%s|%s|%s", dt.getMillis(), userName, token).getBytes());
	}

	public boolean isEmailAlreadyExist(String email) {

		SearchRestriction sr = new TermRestriction<>(UserTermKeys.EMAIL, email);
		try {

			List<String> users = client.searchUserNames(sr, 0, 1);

			return !users.isEmpty();

		} catch (OperationFailedException | InvalidAuthenticationException
				| ApplicationPermissionException e) {

			log.error("Exception during crowd API call", e);
			throw new SSOServiceException(e.getMessage());
		}

	}
}