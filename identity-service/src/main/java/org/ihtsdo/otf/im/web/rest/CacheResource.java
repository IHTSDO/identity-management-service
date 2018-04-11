package org.ihtsdo.otf.im.web.rest;

import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.service.CacheManagerService;
import org.ihtsdo.otf.im.service.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST controller for managing cache.
 */
@RestController
@RequestMapping("/api")
public class CacheResource {

	public static final String ROLE_CROWD_ADMINISTRATORS = "ROLE_ims-administrators";

	private final Logger log = LoggerFactory.getLogger(CacheResource.class);

	@Autowired
	private CacheManagerService cacheManagerService;

	@Autowired
	private UserDetailsService userRepository;

	/**
	 * POST  /cache/clear-all -> clear cache for all users.
	 */
	@RequestMapping(value = "/cache/clear-all",
			method = RequestMethod.POST)
	public void getAll(HttpServletResponse response) {
		try {
			log.info("REST request to clear all caches");

			IHTSDOUser currentUser = userRepository.getCurrentUser();
			Assert.notNull(currentUser, "User must be logged in.");

			if (hasAuthority(currentUser, ROLE_CROWD_ADMINISTRATORS)) {
				log.info("Clearing all caches.");
				cacheManagerService.clearAll();
				response.setStatus(204);
			} else {
				log.info("User {} does not have required role {}", currentUser.getUsername(), ROLE_CROWD_ADMINISTRATORS);
				response.setStatus(401);
				response.getWriter().write("Current user does not have required role " + ROLE_CROWD_ADMINISTRATORS);
			}
		} catch (IOException e) {
			log.error("Error handling clear cache request.");
		}
	}

	private boolean hasAuthority(IHTSDOUser user, String role) {
		for (GrantedAuthority grantedAuthority : user.getAuthorities()) {
			if (grantedAuthority.getAuthority().equals(role)) {
				return true;
			}
		}
		return false;
	}

}
