package org.snomed.ims.domain.crowd;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.snomed.ims.domain.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UsersCollection response from Crowd. Essentially, a collection of users.
 */
public class UsersCollection {
	@JsonProperty("expand")
	private String expand;

	@JsonProperty("users")
	private List<User> users;

	@JsonProperty("name")
	private String name;

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public List<User> getUsers() {
		if (users == null) {
			return Collections.emptyList();
		}

		return users;
	}

	public boolean hasOneUser() {
		return name != null && (users == null || users.isEmpty());
	}

	public UsersCollection setUsers(List<User> users) {
		this.users = users;
		return this;
	}

	public void addUser(User user) {
		if (users == null) {
			users = new ArrayList<>();
		}

		users.add(user);
	}

	public String getName() {
		return name;
	}

	public UsersCollection setName(String name) {
		this.name = name;
		return this;
	}
}
