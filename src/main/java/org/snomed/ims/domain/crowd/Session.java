package org.snomed.ims.domain.crowd;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Session response from Crowd.
 */
public class Session {
	@JsonProperty("expand")
	private String expand;

	@JsonProperty("token")
	private String token;

	@JsonProperty("user")
	private User user;

	@JsonProperty("created-date")
	private Long createdDate;

	@JsonProperty("expiry-date")
	private Long expiryDate;

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Long getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Long expiryDate) {
		this.expiryDate = expiryDate;
	}
}
