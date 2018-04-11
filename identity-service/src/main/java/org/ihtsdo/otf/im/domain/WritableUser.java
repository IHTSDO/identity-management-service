package org.ihtsdo.otf.im.domain;

import com.atlassian.crowd.embedded.api.UserComparator;
import com.atlassian.crowd.model.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.Email;
import org.springframework.security.core.GrantedAuthority;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * A user. This object should only used inside identity service
 */
public class WritableUser implements Serializable, User {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@NotNull
	@Pattern(regexp = "^[a-z0-9]*$")
	@Size(min = 1, max = 50)
	private String userName;

	private String fullName;

	@JsonIgnore
	@NotNull
	@Size(min = 5, max = 100)
	private String password;

	@Size(max = 50)
	private String firstName;

	@Size(max = 50)
	private String lastName;

	@Email
	@Size(max = 100)
	private String email;

	private boolean activated = false;

	@Size(min = 2, max = 5)
	private String langKey;

	@Size(max = 200)
	private String activationKey;

	private String passwordResetKey;

	@JsonIgnore
	private Collection<? extends GrantedAuthority> authorities = new HashSet<>();

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean getActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public String getActivationKey() {
		return activationKey;
	}

	public void setActivationKey(String activationKey) {
		this.activationKey = activationKey;
	}

	public String getLangKey() {
		return langKey;
	}

	public void setLangKey(String langKey) {
		this.langKey = langKey;
	}

	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		WritableUser user = (WritableUser) o;

		return userName.equals(user.userName);
	}

	@Override
	public int hashCode() {

		return UserComparator.hashCode(this);
	}

	@Override
	public String toString() {
		return "User{" +
				"userName='" + userName + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", email='" + email + '\'' +
				", activated='" + activated + '\'' +
				", langKey='" + langKey + '\'' +
				", activationKey='" + activationKey + '\'' +
				"}";
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the fullName
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * @param fullName the fullName to set
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/* (non-Javadoc)
	 * @see java.security.Principal#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return this.userName;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.embedded.api.User#getDirectoryId()
	 */
	@Override
	public long getDirectoryId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.embedded.api.User#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return this.fullName;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.embedded.api.User#getEmailAddress()
	 */
	@Override
	public String getEmailAddress() {
		// TODO Auto-generated method stub
		return this.email;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.embedded.api.User#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return this.activated;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.model.user.User#getExternalId()
	 */
	@Override
	public String getExternalId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.crowd.embedded.api.User#compareTo(com.atlassian.crowd.embedded.api.User)
	 */
	@Override
	public int compareTo(com.atlassian.crowd.embedded.api.User arg0) {
		// TODO Auto-generated method stub
		return UserComparator.compareTo(this, arg0);
	}

	/**
	 * @return the passwordResetKey
	 */
	public String getPasswordResetKey() {
		return passwordResetKey;
	}

	/**
	 * @param passwordResetKey the passwordResetKey to set
	 */
	public void setPasswordResetKey(String passwordResetKey) {
		this.passwordResetKey = passwordResetKey;
	}

}
