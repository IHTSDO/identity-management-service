package org.ihtsdo.otf.im.service;

import net.sf.ehcache.Cache;
import org.ihtsdo.otf.im.web.rest.CacheResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CacheManagerService {

	private final Set<Cache> cacheSet = new HashSet<>();
	private final Logger log = LoggerFactory.getLogger(CacheResource.class);

	public void addCache(Cache cache) {
		synchronized (cacheSet) {
			cacheSet.add(cache);
		}
	}

	public void clearAll() {
		synchronized (cacheSet) {
			for (Cache cache : cacheSet) {
				log.info("Clearing {} items from cache {}", cache.getSize(), cache.getName());
				cache.removeAll();
			}
		}
	}
}
