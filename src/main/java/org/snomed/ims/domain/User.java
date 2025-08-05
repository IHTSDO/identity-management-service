package org.snomed.ims.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * User response from Crowd.
 */
@JsonSerialize(using = UserView.class)
public class User implements Serializable {
	private static final long serialVersionUID = 1L;

	private String id;

	@JsonProperty("name")
	private String login;

	@JsonProperty("key")
	private String langKey;

	@JsonProperty("active")
	private Boolean active;

	@JsonProperty("first-name")
	private String firstName;

	@JsonProperty("last-name")
	private String lastName;

	@JsonProperty("display-name")
	private String displayName;

	@JsonProperty("email")
	private String email;

	private List<String> roles;

	public User() {
	}

	public User(String login, String firstName, String lastName, String email, String langKey,
				List<String> roles) {
		this.login = login;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.langKey = langKey;
		this.roles = roles;
	}

	public User publicClone() {
		User user = new User();
		user.setLogin(login);
		user.setDisplayName(displayName);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		return user;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public String getId() {
		return id;
	}

	public String getLogin() {
		return login;
	}

	public User setLogin(String login) {
		this.login = login;
		return this;
	}

	public String getLangKey() {
		return langKey;
	}

	public void setLangKey(String langKey) {
		this.langKey = langKey;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		if (active == null) {
			return false;
		}

		return Boolean.TRUE.equals(active);
	}

	public String getFirstName() {
		return firstName;
	}

	public User setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public User setEmail(String email) {
		this.email = email;
		return this;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(login, user.login);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(login);
	}

	@Override
	public String toString() {
		return "User{" +
				"login='" + login + '\'' +
				", langKey='" + langKey + '\'' +
				", active=" + active +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", displayName='" + displayName + '\'' +
				", email='" + email + '\'' +
				", roles=" + roles +
				'}';
	}
}
