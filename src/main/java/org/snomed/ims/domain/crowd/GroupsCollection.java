package org.snomed.ims.domain.crowd;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * GroupsCollection response from Crowd. Essentially, a collection of groups.
 */
public class GroupsCollection {
	@JsonProperty("expand")
	private String expand;

	@JsonProperty("groups")
	private List<Group> groups;

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public List<Group> getGroups() {
		return groups;
	}

	public GroupsCollection setGroups(List<Group> groups) {
		this.groups = groups;
		return this;
	}

	public List<String> getGroupNames() {
		if (groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}

		return groups.stream().map(Group::getName).toList();
	}
}
