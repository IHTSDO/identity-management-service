package org.ihtsdo.otf.im.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.domain.WritableUser;
import org.ihtsdo.otf.im.error.UserNotFoundException;
import org.ihtsdo.otf.im.security.InvalidKeyException;
import org.ihtsdo.otf.im.security.SecurityUtils;
import org.ihtsdo.otf.im.service.MailService;
import org.ihtsdo.otf.im.service.UserDetailsService;
import org.ihtsdo.otf.im.service.UserService;
import org.ihtsdo.otf.im.web.rest.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

	private final Logger log = LoggerFactory.getLogger(AccountResource.class);

	@Autowired
	private UserDetailsService userRepository;

	@Inject
	private UserService userService;

	@Inject
	private MailService mailService;

	@Autowired
	private ObjectMapper objectMapper;

	public static final String AUTH_HEADER_PREFIX = "X-AUTH-";
	public static final String AUTH_HEADER_USERNAME = AUTH_HEADER_PREFIX + "username";


	/**
	 * POST  /register -> register the user.
	 */
	@RequestMapping(value = "/register",
			method = RequestMethod.POST,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> registerAccount(@Valid @RequestBody UserDTO userDTO, HttpServletRequest request) {

		IHTSDOUser user = null;

		try {

			user = userRepository.getUserByUserName(userDTO.getLogin());

		} catch (UserNotFoundException e) {

			//go and register user
		}
		if (user != null) {
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("login already in use");
		} else if (userService.isEmailAlreadyExist(userDTO.getEmail())) {
			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("e-mail address already in use");
		}

		WritableUser newUSer = userService.createUserInformation(userDTO.getLogin(), userDTO.getPassword(),
				userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail().toLowerCase(),
				userDTO.getLangKey());
		String baseUrl = getBaseUrl(request);

		mailService.sendActivationEmail(newUSer, baseUrl);
		return new ResponseEntity<>(HttpStatus.CREATED);

	}
	
	/**
	 * POST  /pre-register -> check if the username or email address is already registered.
	 * NB Used by MLDS to decide if user should login using IMS or MLDS specific user database.
	 */
	@RequestMapping(value = "/pre-register-check",
			method = RequestMethod.POST,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> checkAccountAvailable(@RequestParam String username, 
			@RequestParam(required = false) String emailAddr, 
			HttpServletRequest request) {

		try {
			userRepository.getUserByUserName(username);
		} catch (UserNotFoundException e) {
			if (emailAddr != null && userService.isEmailAlreadyExist(emailAddr)) {
				return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("e-mail address already in use");
			}
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("login already in use");
	}

	/**
	 * @param request
	 * @return
	 */
	private String getBaseUrl(HttpServletRequest request) {
		String url = request.getScheme() + // "http"
				"://" +                            // "://"
				request.getServerName() +          // "myhost"
				":" +                              // ":"
				request.getServerPort();           // "80"		return null;
		return url;
	}

	/**
	 * GET  /activate -> activate the registered user.
	 */
	@RequestMapping(value = "/activate",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> activateAccount(@RequestParam(value = "key") String key) {
		IHTSDOUser user = userService.activateRegistration(key);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * GET  /authenticate -> check if the user is authenticated, and return its login.
	 */
	@RequestMapping(value = "/authenticate",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public String isAuthenticated(HttpServletRequest request) {
		log.debug("REST request to check if the current user is authenticated");
		return request.getRemoteUser();
	}


	/**
	 * GET  /account -> get the current user.
	 */
	@RequestMapping(value = "/account",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public void getAccount(HttpServletResponse response) throws IOException {
		IHTSDOUser user = userRepository.getCurrentUser();
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		List<String> roles = new ArrayList<>();
		for (GrantedAuthority authority : user.getAuthorities()) {
			roles.add(authority.getAuthority());
		}

		response.setStatus(200);
		response.setHeader("Content-Type", "application/json;charset=UTF-8");
		response.setHeader(AUTH_HEADER_USERNAME, user.getUsername());
		response.setHeader(AUTH_HEADER_PREFIX + "firstName", user.getFirstName());
		response.setHeader(AUTH_HEADER_PREFIX + "lastName", user.getLastName());
		response.setHeader(AUTH_HEADER_PREFIX + "emailAddress", user.getEmailAddress());
		response.setHeader(AUTH_HEADER_PREFIX + "langKey", user.getLangKey());
		response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(roles, ","));

		UserDTO userDTO = new UserDTO(
				user.getUsername(),
				null,
				user.getFirstName(),
				user.getLastName(),
				user.getEmailAddress(),
				user.getLangKey(),
				roles);
		objectMapper.writeValue(response.getOutputStream(), userDTO);
	}

	/**
	 * POST  /account -> update the current user information.
	 */
	@RequestMapping(value = "/account",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> saveAccount(@RequestBody UserDTO userDTO) {
		IHTSDOUser userHavingThisLogin = userRepository.getUserByUserName(userDTO.getLogin());
		if (userHavingThisLogin != null && !userHavingThisLogin.getUsername().equals(SecurityUtils.getCurrentLogin())) {

			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

		} else if (userService.isEmailAlreadyExist(userDTO.getEmail()) && !(userHavingThisLogin.getEmailAddress() != null
				&& userHavingThisLogin.getEmailAddress().equalsIgnoreCase(userDTO.getEmail()))) {

			return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("e-mail address already in use");

		}

		userService.updateUserInformation(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(), userDTO.getLangKey());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * POST  /change_password -> changes the current user's password
	 */
	@RequestMapping(value = "/account/change_password",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> changePassword(@RequestBody String password) {
		if (StringUtils.isEmpty(password) || password.length() < 5 || password.length() > 50) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		userService.changePassword(password);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * POST  /logout -> filter intercepts this and logs out user
	 */
	@RequestMapping(value = "/logout",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> logoutPost() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * POST  /forgot_password -> sends a resets password link to the current user's email id
	 */
	@RequestMapping(value = "/forgot_password",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> requestPasswordReset(@RequestBody String userName, HttpServletRequest request) {
		log.debug("resetPassword for User {}", userName);

		if (StringUtils.isEmpty(userName) || userName.length() < 5 || userName.length() > 50) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {

			WritableUser user = userService.requestPasswordReset(userName);

			mailService.sendPasswordResetEmail(user, getBaseUrl(request));


		} catch (UserNotFoundException e) {

			log.info("User {} does not exist. hence no password is being reset. System error {}", userName, e.getMessage());

		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * GET  /reset_password -> validate password link and redirect user to change_password page.
	 */
	@RequestMapping(value = "/reset_password",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> validatePasswordResetLink(@RequestParam(value = "password") String password,
															@RequestParam(value = "key") String key,
															HttpServletRequest request) {

		try {

			WritableUser user = userService.validatePasswordResetKey(key, password);
			if (user == null) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			//send email of successful password reset
			mailService.sendPasswordResetSuccessEmail(user, getBaseUrl(request));

		} catch (InvalidKeyException e) {

			log.error("Invalid key", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

		}

		return new ResponseEntity<>(HttpStatus.OK);

	}


}
