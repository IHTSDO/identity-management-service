package org.ihtsdo.otf.im.rest;

import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.sf.ehcache.CacheManager;

/**
 * REST controller for managing cache.
 */
@RestController
@RequestMapping("/api")
public class CacheResource {

	/**
	 * POST  /cache/clear-all -> clear cache for all users.
	 */
	@RequestMapping(value = "/cache/clear-all",
			method = RequestMethod.POST)
	
	@Secured({"ROLE_ims-administrators"})
	public void clearCache(HttpServletResponse response) {
		 CacheManager.getInstance().clearAll();
	}

}
