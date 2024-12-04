package org.ihtsdo.otf.im.service.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupsResponse {

	private List<Group> groups;

	public List<Group> getGroups() {
		return groups;
	}

	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	public Set<String> getGroupNames() {
		return groups.stream().map(Group::getName).collect(Collectors.toSet());
	}
}
