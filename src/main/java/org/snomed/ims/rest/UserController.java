package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.ims.domain.crowd.User;
import org.snomed.ims.domain.crowd.UserUpdateRequest;
import org.snomed.ims.middle.CrowdRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "UserController")
public class UserController {

	private final CrowdRestClient crowdRestClient;

	public UserController(CrowdRestClient crowdRestClient) {
		this.crowdRestClient = crowdRestClient;
	}

	@GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> getUserDetails(@RequestParam String username) {
		User user = crowdRestClient.getUser(username);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(user, HttpStatus.OK);
		}
	}

	@PutMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<User> updateUser(@RequestParam String username, @RequestBody UserUpdateRequest requestBody) {
		User user = crowdRestClient.getUser(username);
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(crowdRestClient.updateUser(user, requestBody), HttpStatus.OK);
		}
	}

	@GetMapping(value = "/user/role", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<String>> getUserRoles(@RequestParam String username) {
		return new ResponseEntity<>(crowdRestClient.getUserRoles(username), HttpStatus.OK);
	}

}
