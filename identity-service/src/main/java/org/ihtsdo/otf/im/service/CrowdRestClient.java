package org.ihtsdo.otf.im.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.ihtsdo.otf.im.rest.dto.UserDTO;
import org.ihtsdo.otf.im.security.AuthoritiesConstants;
import org.ihtsdo.otf.im.service.model.GroupsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@SuppressWarnings("rawtypes")
public class CrowdRestClient {

	@Value("${crowd.api.url}")
	private String crowdApiUrl;

	@Value("${crowd.api.auth.application-name}")
	private String crowdApiUsername;

	@Value("${crowd.api.auth.application-password}")
	private String crowdApiPassword;

	private RestTemplate restTemplate;
	
	@PostConstruct
	public void init() {
		restTemplate = new RestTemplateBuilder()
				.rootUri(crowdApiUrl)
				.basicAuthentication(crowdApiUsername, crowdApiPassword)
				.build();
	}

	public void getGroups(String username) {
		Map<String, String> params = new HashMap<>();
		params.put("username", username);
		GroupsResponse groupsResponse = restTemplate.getForObject("/user/group/direct.json?username={username}", GroupsResponse.class, params);
		System.out.println(groupsResponse.getGroupNames());
	}
	
	public UserDTO getUser(String username) {
		Map<String, String> params = new HashMap<>();
		params.put("username", username);
		return restTemplate.getForObject("/user?username={username}", UserDTO.class, params);
	}

	public String authenticate(String username, String password) {
		Map<String, String> params = new HashMap<>();
		params.put("username", username);
		params.put("password", password);
		
		Map response = restTemplate.postForObject("/session", params, Map.class);
		if (null != response) {
			String token = (String) response.get("token");
			return token;
		}
		
		return "";
	}
	
	@Cacheable(value="accountCache", key="#token")
	public UserDTO getUserByToken(String token){
		UserDTO userDTO = new UserDTO();
		Map<String, String> params = new HashMap<>();
		params.put("token", token);
		
		Map result = restTemplate.getForObject("/session/{token}", Map.class, params);
		if (null != result) {
			
			// Get user information
			Map user = (Map) result.get("user");
			userDTO.setFirstName(user.get("first-name").toString());
			userDTO.setLastName(user.get("last-name").toString());
			userDTO.setEmail(user.get("email").toString());
			userDTO.setLangKey(user.get("key").toString());
			userDTO.setLogin(user.get("name").toString());
			
			// Get all roles of user
			params.clear();
			params.put("username", user.get("name").toString());
			result = restTemplate.getForObject("/user/group/direct?username={username}", Map.class, params);
			if (null != result) {
				ArrayList<?> arrRoles = (ArrayList<?>) result.get("groups");
				List<String> lstRoles = new ArrayList<String>();
				for (int i = 0; i < arrRoles.size(); i++) {
					Map role =  (Map) arrRoles.get(i);
					lstRoles.add(AuthoritiesConstants.ROLE_PREFIX + role.get("name"));
				}
				userDTO.setRoles(lstRoles);
			}
		}
		
		return userDTO;
	}
	
	@CacheEvict(value = "accountCache", key = "#token")
	public void invalidateToken(String token) {
		restTemplate.delete("/session/{token}", token);	
	}
}
