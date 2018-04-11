package org.ihtsdo.otf.im.rest;

import org.ihtsdo.otf.im.rest.dto.UserDTO;
import org.ihtsdo.otf.im.service.CrowdRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

	private final Logger log = LoggerFactory.getLogger(AccountResource.class);
	
	@Value("${cookie.name}")
	private String cookieName;
	
	@Value("${cookie.maxAge}")
	private int cookieMaxAge;
	
	@Value("${cookie.domain}")
	private String cookieDomain;

	@Autowired
	private CrowdRestClient crowdRestClient;

	public static final String AUTH_HEADER_PREFIX = "X-AUTH-";
	public static final String AUTH_HEADER_USERNAME = AUTH_HEADER_PREFIX + "username";


	private String getBaseUrl(HttpServletRequest request) {
		String url = request.getScheme() + // "http"
				"://" +                            // "://"
				request.getServerName() +          // "myhost"
				":" +                              // ":"
				request.getServerPort();           // "80"		return null;
		return url;
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
	 * POST  /authenticate -> validate user.
	 */
	@RequestMapping(value = "/authenticate",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Cookie> validateUser(@RequestBody UserDTO dto, HttpServletResponse response) {
		try {
			String token = crowdRestClient.authenticate(dto.getLogin(), dto.getPassword());
			Cookie cookie= new Cookie(cookieName, token);
			cookie.setMaxAge(cookieMaxAge);
			cookie.setDomain(cookieDomain);
			response.addCookie(cookie);
			return new ResponseEntity<Cookie>(cookie, HttpStatus.OK);
		} catch (RestClientException ex) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	/**
	 * POST /account/logout  --> logout
	 */
	
	@RequestMapping(value = "/account/logout",
			method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (request.isRequestedSessionIdValid() && session != null) {
			session.invalidate();
		}

		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0){
				cookie.setMaxAge(0);
				cookie.setValue("");
				cookie.setPath("/");
				response.addCookie(cookie);
			}
		}
	}
		
	
	/**
	 * GET  /account -> get the current user.
	 */
	@RequestMapping(value = "/account",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<UserDTO> getAccount(HttpServletRequest request) {
		try {
			UserDTO userDTO = new UserDTO();
	        Cookie[] cookies = request.getCookies();
	        for (Cookie cookie : cookies) {
	            if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
	            	userDTO = crowdRestClient.getUserByToken(cookie.getValue());
	            	return new ResponseEntity<UserDTO>(userDTO, HttpStatus.OK);
	            }
	        }
	        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		} catch (RestClientException ex) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	

	/**
	 * GET  /account -> get the current user.
	 */
//	@RequestMapping(value = "/account",
//			method = RequestMethod.GET,
//			produces = MediaType.APPLICATION_JSON_VALUE)
//	public void getAccount(HttpServletResponse response) throws IOException {
//		IHTSDOUser user = userRepository.getCurrentUser();
//		if (user == null) {
//			log.info("Current user not recovered.  Setting internal error");
//			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//		}
//		List<String> roles = new ArrayList<>();
//		for (GrantedAuthority authority : user.getAuthorities()) {
//			roles.add(authority.getAuthority());
//		}
//
//		response.setStatus(200);
//		response.setHeader("Content-Type", "application/json;charset=UTF-8");
//		response.setHeader(AUTH_HEADER_USERNAME, user.getUsername());
//		response.setHeader(AUTH_HEADER_PREFIX + "firstName", user.getFirstName());
//		response.setHeader(AUTH_HEADER_PREFIX + "lastName", user.getLastName());
//		response.setHeader(AUTH_HEADER_PREFIX + "emailAddress", user.getEmailAddress());
//		response.setHeader(AUTH_HEADER_PREFIX + "langKey", user.getLangKey());
//		response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(roles, ","));
//
//		UserDTO userDTO = new UserDTO(
//				user.getUsername(),
//				null,
//				user.getFirstName(),
//				user.getLastName(),
//				user.getEmailAddress(),
//				user.getLangKey(),
//				roles);
//	}

	/**
	 * POST  /logout -> filter intercepts this and logs out user
	 */
	@RequestMapping(value = "/logout",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> logoutPost() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
