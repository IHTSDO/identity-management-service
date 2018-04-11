package org.ihtsdo.otf.im.web.rest;

import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.security.AuthoritiesConstants;
import org.ihtsdo.otf.im.sso.service.IHTSDOUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for managing users.
 */
@RestController
@RequestMapping("/api")
public class UserResource {

	private final Logger log = LoggerFactory.getLogger(UserResource.class);

	@Inject
	private IHTSDOUserDetailsService userRepository;

	/**
	 * GET  /users -> get all users.
	 */
	@RequestMapping(value = "/users",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public List<IHTSDOUser> getAll() {
		log.debug("REST request to get all Users");
		return new ArrayList<>();//userRepository.findAll();
	}

	/**
	 * GET  /users/:login -> get the "login" user.
	 */
	@RequestMapping(value = "/users/{login}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@RolesAllowed(AuthoritiesConstants.IHTDO_OPS_ADMIN)
	public IHTSDOUser getUser(@PathVariable String login, HttpServletResponse response) {
		log.debug("REST request to get User : {}", login);
		IHTSDOUser user = userRepository.getUserByUserName(login);
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		return user;
	}
}
