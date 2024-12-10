package org.ihtsdo.otf.im.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO implements Serializable{

	private static final long serialVersionUID = 1L;

	@Pattern(regexp = "^[a-z0-9]*$")
	@Size(min = 1, max = 50)
    @NotNull
	private String login;

	@Size(min = 5, max = 100)
	private String password;

	@Size(max = 50)
    @NotNull
	private String firstName;

	@Size(max = 50)
	@NotNull
	private String lastName;

	@Email
	@NotNull
	@Size(min = 5, max = 100)
	private String email;

	@Size(min = 2, max = 5)
	private String langKey;

	private String displayName;

	private boolean active;

	private List<String> roles;

	public UserDTO() {
	}

	public UserDTO(String login, String password, String firstName, String lastName, String email, String langKey,
				   List<String> roles) {
		this.login = login;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.langKey = langKey;
		this.roles = roles;
	}

	public String getLogin() {
		return login;
	}

	public String getUsername() {
		return this.getLogin();
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setLangKey(String langKey) {
		this.langKey = langKey;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public String getPassword() {
		return password;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getLangKey() {
		return langKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isActive() {
		return active;
	}

	public List<String> getRoles() {
		return roles;
	}

	@Override
	public String toString() {
		return "UserDTO{" +
				"login='" + login + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", email='" + email + '\'' +
				", langKey='" + langKey + '\'' +
				", roles=" + roles +
				'}';
	}
}
