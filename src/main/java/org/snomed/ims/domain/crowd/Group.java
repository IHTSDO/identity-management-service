package org.snomed.ims.domain.crowd;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Group response from Crowd.
 */
public class Group {
	@JsonProperty("name")
	private String name;

	public String getName() {
		return name;
	}

	public Group setName(String name) {
		this.name = name;
		return this;
	}
}
