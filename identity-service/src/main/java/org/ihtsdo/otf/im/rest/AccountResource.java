package org.ihtsdo.otf.im.rest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
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
		if (StringUtils.isEmpty(dto.getLogin()) || StringUtils.isEmpty(dto.getPassword())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {			
			String token = crowdRestClient.authenticate(dto.getLogin(), dto.getPassword());
			Cookie cookie= new Cookie(cookieName, token);
			cookie.setMaxAge(cookieMaxAge);
			cookie.setDomain(cookieDomain);
			response.addCookie(cookie);
			return new ResponseEntity<Cookie>(cookie, HttpStatus.OK);
		} catch (RestClientException ex) {
			ex.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	/**
	 * POST /account/logout  --> logout
	 */
	
	@RequestMapping(value = "/account/logout",
			method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (request.isRequestedSessionIdValid() && session != null) {
			session.invalidate();
		}

		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
				if (StringUtils.isNotEmpty(cookie.getValue())) {
					try {
						crowdRestClient.invalidateToken(cookie.getValue());
					} catch (RestClientException ex) {
						log.error("Token {} is not valid", cookie.getValue());
					}
				}
				
				// invalidate cookie
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
	public ResponseEntity<UserDTO> getAccount(HttpServletRequest request, HttpServletResponse response) {
		UserDTO userDTO;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
        	for (Cookie cookie : cookies) {
	            if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
	            	try {
	            		userDTO = crowdRestClient.getUserByToken(cookie.getValue());
	            	} catch  (RestClientException ex) {
	        			ex.printStackTrace();
	        			
	        			// invalidate cookie
	    				cookie.setMaxAge(0);
	    				cookie.setValue("");
	    				cookie.setPath("/");
	    				response.addCookie(cookie);
	    				
	        			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	        		}
	            	
	            	// Set response header
	        		response.setHeader("Content-Type", "application/json;charset=UTF-8");
	        		response.setHeader(AUTH_HEADER_USERNAME, userDTO.getLogin());
	        		response.setHeader(AUTH_HEADER_PREFIX + "roles", StringUtils.join(userDTO.getRoles(), ","));
	        		
	            	return new ResponseEntity<UserDTO>(userDTO, HttpStatus.OK);
	            }
	        }
        }
        
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

}
