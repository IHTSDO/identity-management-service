package org.snomed.ims.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationResponse {
    
    @JsonProperty("authenticated")
    private final boolean authenticated;
    
    @JsonProperty("loginUrl")
    private final String loginUrl;
    
    @JsonProperty("user")
    private final User user;
    
    public AuthenticationResponse(boolean authenticated, String loginUrl, User user) {
        this.authenticated = authenticated;
        this.loginUrl = loginUrl;
        this.user = user;
    }
    
    public static AuthenticationResponse authenticated(User user) {
        return new AuthenticationResponse(true, null, user);
    }
    
    public static AuthenticationResponse unauthenticated(String loginUrl) {
        return new AuthenticationResponse(false, loginUrl, null);
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getLoginUrl() {
        return loginUrl;
    }
    
    public User getUser() {
        return user;
    }
}
