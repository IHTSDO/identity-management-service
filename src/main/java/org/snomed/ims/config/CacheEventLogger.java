package org.snomed.ims.config;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.snomed.ims.domain.crowd.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("rawtypes")
public class CacheEventLogger implements CacheEventListener<Object, Object> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheEventLogger.class);

	@Override
	public void onEvent(CacheEvent cacheEvent) {
		Object newValue = cacheEvent.getNewValue();

		if (newValue == null) {
			Object oldValue = cacheEvent.getOldValue();
			traceUser(cacheEvent, oldValue);
		} else {
			traceUser(cacheEvent, newValue);
		}
	}

	private void traceUser(CacheEvent cacheEvent, Object object) {
		if (object instanceof User user) {
			LOGGER.trace("CACHE Event={}, login={}", cacheEvent.getType(), user.getLogin());
		}
	}
}
