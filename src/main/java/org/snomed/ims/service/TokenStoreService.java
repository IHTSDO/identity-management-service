package org.snomed.ims.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Service to manage access tokens using session IDs
 * This avoids storing large tokens in cookies which can exceed browser limits
 */
@Service
public class TokenStoreService {
    
    // In-memory token storage (in production, use Redis or database)
    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * Store an access token and return a session ID
     * @param accessToken the access token to store
     * @return the session ID to use in cookies
     */
    public String storeToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return null;
        }
        String sessionId = UUID.randomUUID().toString();
        tokenStore.put(sessionId, accessToken);
        return sessionId;
    }
    
    /**
     * Get access token by session ID
     * @param sessionId the session ID from the cookie
     * @return the access token or null if not found
     */
    public String getAccessToken(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        return tokenStore.get(sessionId);
    }
    
    /**
     * Remove access token by session ID (for logout)
     * @param sessionId the session ID to remove
     */
    public void removeAccessToken(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            tokenStore.remove(sessionId);
        }
    }
    
    /**
     * Check if a session ID exists
     * @param sessionId the session ID to check
     * @return true if the session ID exists
     */
    public boolean hasSession(String sessionId) {
        return sessionId != null && !sessionId.isEmpty() && tokenStore.containsKey(sessionId);
    }
}
