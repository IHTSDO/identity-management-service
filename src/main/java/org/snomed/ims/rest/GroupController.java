package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.middle.CrowdRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "GroupController")
public class GroupController {
	private final CrowdRestClient crowdRestClient;

	public GroupController(CrowdRestClient crowdRestClient) {
		this.crowdRestClient = crowdRestClient;
	}

	@GetMapping(value = "/group/user",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<User>> getUserByGroup(@RequestParam String groupname,
													 @RequestParam(required = false) String username,
													 @RequestParam int maxResults,
													 @RequestParam int startAt) {
		List<User> users = crowdRestClient.searchUsersByGroup(groupname, username, maxResults, startAt);
		return new ResponseEntity<>(users, HttpStatus.OK);
	}
}
