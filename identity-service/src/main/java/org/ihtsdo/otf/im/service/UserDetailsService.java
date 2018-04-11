package org.ihtsdo.otf.im.service;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.security.SecurityUtils;
import org.ihtsdo.otf.im.sso.service.IHTSDOUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public class UserDetailsService {

	private final Cache userDetailsCache;

	@Autowired
	private IHTSDOUserDetailsService userDetailsService;

	public UserDetailsService(Cache userDetailsCache) {
		Assert.notNull(userDetailsCache);
		this.userDetailsCache = userDetailsCache;
	}

	public IHTSDOUser getUserByUserName(String login) {
		Element cached = userDetailsCache.getQuiet(login);
		if (cached != null) {
			return (IHTSDOUser) cached.getObjectValue();
		}
		IHTSDOUser user = userDetailsService.getUserByUserName(login);
		userDetailsCache.put(new Element(login, user));
		return user;
	}

	public IHTSDOUser getCurrentUser() {
		return getUserByUserName(SecurityUtils.getCurrentLogin());
	}

	public void logoutUsername(String username) {
		userDetailsCache.remove(username);
	}
}
