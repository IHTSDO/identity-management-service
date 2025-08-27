package org.snomed.ims.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuthenticationResponse {
    
    @JsonProperty("authenticated")
    private final boolean authenticated;
    
    @JsonProperty("loginUrl")
    private final String loginUrl;
    
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
    
    // User properties at the top level for backward compatibility
    @JsonProperty("login")
    public String getLogin() {
        return user != null ? user.getLogin() : null;
    }
    
    @JsonProperty("firstName")
    public String getFirstName() {
        return user != null ? user.getFirstName() : null;
    }
    
    @JsonProperty("lastName")
    public String getLastName() {
        return user != null ? user.getLastName() : null;
    }
    
    @JsonProperty("email")
    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }
    
    @JsonProperty("displayName")
    public String getDisplayName() {
        return user != null ? user.getDisplayName() : null;
    }
    
    @JsonProperty("active")
    public Boolean getActive() {
        return user != null ? user.getActive() : null;
    }
    
    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getLogin() : null;
    }
    
    @JsonProperty("roles")
    public List<String> getRoles() {
        return user != null ? user.getRoles() : null;
    }

    @JsonProperty("clientAccess")
    public List<String> getClientAccess() {
        return user != null ? user.getClientAccess() : null;
    }
}
