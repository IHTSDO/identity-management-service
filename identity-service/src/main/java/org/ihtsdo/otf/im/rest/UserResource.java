package org.ihtsdo.otf.im.rest;

import org.ihtsdo.otf.im.rest.dto.UserDTO;
import org.ihtsdo.otf.im.service.CrowdRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class UserResource {

    @Autowired
    private CrowdRestClient crowdRestClient;

    @GetMapping(value = "/user",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserDTO> getUserDetails(@RequestParam String username, HttpServletResponse response) {
        try {
            UserDTO user = crowdRestClient.getUser(username);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (RestClientException ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
