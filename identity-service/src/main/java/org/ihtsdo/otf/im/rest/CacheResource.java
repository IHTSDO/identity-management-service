package org.ihtsdo.otf.im.rest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ihtsdo.otf.im.rest.dto.UserDTO;
import org.ihtsdo.otf.im.security.AuthoritiesConstants;
import org.ihtsdo.otf.im.service.CrowdRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.sf.ehcache.CacheManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing cache.
 */
@RestController
@RequestMapping("/api")
public class CacheResource {

	private final Logger log = LoggerFactory.getLogger(CacheResource.class);

	@Value("${cookie.name}")
	private String cookieName;

	@Autowired
	private CrowdRestClient crowdRestClient;

	/**
	 * POST  /cache/clear-all -> clear cache for all users.
	 */
	@RequestMapping(value = "/cache/clear-all",
			method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void clearCache(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName) && cookie.getMaxAge() != 0) {
					try {
						UserDTO user = crowdRestClient.getUserByToken(cookie.getValue());
						if (user.getRoles().contains(AuthoritiesConstants.IMS_ADMIN)) {
							CacheManager.getInstance().clearAll();
							log.info("User {} cleared cache", user.getLogin());
							return;
						}
					} catch (RestClientException ex) {
						log.error(ex.getMessage());

						// invalidate cookie
						cookie.setMaxAge(0);
						cookie.setValue("");
						cookie.setPath("/");
						response.addCookie(cookie);

						throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
					}
				}
			}
		}
		log.error("Not authorised");
		throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}
}
