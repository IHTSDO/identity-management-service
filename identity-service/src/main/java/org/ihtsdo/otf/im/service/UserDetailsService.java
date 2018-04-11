package org.ihtsdo.otf.im.service;

public class UserDetailsService {

//	private final Cache userDetailsCache;
//
//	public UserDetailsService(Cache userDetailsCache) {
//		Assert.notNull(userDetailsCache, "User details cache is required");
//		this.userDetailsCache = userDetailsCache;
//	}
//
//	public IHTSDOUser getUserByUserName(String login) {
//		Element cached = userDetailsCache.getQuiet(login);
//		if (cached != null) {
//			return (IHTSDOUser) cached.getObjectValue();
//		}
//		IHTSDOUser user = doGetUserByUserName(login);
//		userDetailsCache.put(new Element(login, user));
//		return user;
//	}
//
//	public IHTSDOUser getCurrentUser() {
//		return getUserByUserName(SecurityUtils.getCurrentLogin());
//	}
//
//	public void logoutUsername(String username) {
//		userDetailsCache.remove(username);
//	}
//
//	private IHTSDOUser doGetUserByUserName(String login) {
//
//		return null;
//	}
}
