package org.snomed.ims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationProperties {
	@Value("${spring.project.name}")
	private String projectName;

	@Value("${spring.project.version}")
	private String projectVersion;

	@Value("${spring.project.description}")
	private String projectDescription;

	@Value("${cookie.name}")
	private String cookieName;

	@Value("${cookie.maxAge}")
	private String cookieMaxAge;

	@Value("${cookie.domain}")
	private String cookieDomain;

	@Value("${cookie.secure}")
	private String cookieSecureFlag;

	@Value("${keycloak.server.url}")
	private String keycloakUrl;

	@Value("${keycloak.realms}")
	private String keycloakRealms;

	@Value("${keycloak.client-id}")
	private String keycloakClientId;

	@Value("${keycloak.client-secret}")
	private String keycloakClientSecrete;

	@Value("${keycloak.admin.client-id}")
	private String keycloakAdminClientId;

	@Value("${keycloak.admin.client-secret}")
	private String keycloakAdminClientSecret;

	@Value("${crowd.api.url}")
	private String crowdApiUrl;

	@Value("${crowd.api.auth.application-name}")
	private String crowdApiAppName;

	@Value("${crowd.api.auth.application-password}")
	private String crowdApiAppPassword;

	@Value("${basic.auth.enabled}")
	private String basicAuthEnabled;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public String getProjectDescription() {
		return projectDescription;
	}

	public void setProjectDescription(String projectDescription) {
		this.projectDescription = projectDescription;
	}

	public String getCookieName() {
		return cookieName;
	}

	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	public String getCookieMaxAge() {
		return cookieMaxAge;
	}

	public Integer getCookieMaxAgeInt() {
		if (cookieMaxAge == null) {
			return null;
		}

		try {
			return Integer.parseInt(cookieMaxAge);
		} catch (Exception e) {
			return null;
		}
	}

	public void setCookieMaxAge(String cookieMaxAge) {
		this.cookieMaxAge = cookieMaxAge;
	}

	public String getCookieDomain() {
		return cookieDomain;
	}

	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	public String getCookieSecureFlag() {
		return cookieSecureFlag;
	}

	public boolean isCookieSecure() {
		if (cookieSecureFlag == null) {
			return false;
		}

		return "true".equals(cookieSecureFlag);
	}

	public void setCookieSecureFlag(String cookieSecureFlag) {
		this.cookieSecureFlag = cookieSecureFlag;
	}

	public String getKeycloakUrl() {
		return keycloakUrl;
	}

	public void setKeycloakUrl(String keycloakUrl) {
		this.keycloakUrl = keycloakUrl;
	}

	public String getKeycloakRealms() {
		return keycloakRealms;
	}

	public void setKeycloakRealms(String keycloakRealms) {
		this.keycloakRealms = keycloakRealms;
	}

	public String getKeycloakClientId() {
		return keycloakClientId;
	}

	public void setKeycloakClientId(String keycloakClientId) {
		this.keycloakClientId = keycloakClientId;
	}

	public String getKeycloakClientSecrete() {
		return keycloakClientSecrete;
	}

	public void setKeycloakClientSecrete(String keycloakClientSecrete) {
		this.keycloakClientSecrete = keycloakClientSecrete;
	}

	public String getKeycloakAdminClientId() {
		return keycloakAdminClientId;
	}

	public void setKeycloakAdminClientId(String keycloakAdminClientId) {
		this.keycloakAdminClientId = keycloakAdminClientId;
	}

	public String getKeycloakAdminClientSecret() {
		return keycloakAdminClientSecret;
	}

	public void setKeycloakAdminClientSecret(String keycloakAdminClientSecret) {
		this.keycloakAdminClientSecret = keycloakAdminClientSecret;
	}

	public String getCrowdApiUrl() {
		return crowdApiUrl;
	}

	public void setCrowdApiUrl(String crowdApiUrl) {
		this.crowdApiUrl = crowdApiUrl;
	}

	public String getCrowdApiAppName() {
		return crowdApiAppName;
	}

	public void setCrowdApiAppName(String crowdApiAppName) {
		this.crowdApiAppName = crowdApiAppName;
	}

	public String getCrowdApiAppPassword() {
		return crowdApiAppPassword;
	}

	public void setCrowdApiAppPassword(String crowdApiAppPassword) {
		this.crowdApiAppPassword = crowdApiAppPassword;
	}

	public String getBasicAuthEnabled() {
		return basicAuthEnabled;
	}

	public void setBasicAuthEnabled(String basicAuthEnabled) {
		this.basicAuthEnabled = basicAuthEnabled;
	}

	public boolean isBasicAuthEnabled() {
		return "true".equals(basicAuthEnabled);
	}
}
