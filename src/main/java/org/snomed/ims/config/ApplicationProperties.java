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

	@Value("${crowd.api.url}")
	private String crowdApiUrl;

	@Value("${crowd.api.auth.application-name}")
	private String crowdApiAppName;

	@Value("${crowd.api.auth.application-password}")
	private String crowdApiAppPassword;

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
}
