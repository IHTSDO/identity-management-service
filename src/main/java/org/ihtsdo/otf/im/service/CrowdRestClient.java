package org.ihtsdo.otf.im.service;

import org.ihtsdo.otf.im.rest.dto.UserDTO;
import org.ihtsdo.otf.im.security.AuthoritiesConstants;
import org.ihtsdo.otf.im.service.model.GroupsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map result = restTemplate.getForObject("/user?username={username}", Map.class, params);
		UserDTO userDTO = new UserDTO();
		if (null != result) {
			userDTO.setFirstName(result.get("first-name").toString());
			userDTO.setLastName(result.get("last-name").toString());
			userDTO.setEmail(result.get("email").toString());
			userDTO.setLogin(result.get("name").toString());
			userDTO.setDisplayName(result.get("display-name").toString());
			userDTO.setActive(Boolean.parseBoolean(result.get("active").toString()));
		}

		return userDTO;
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
		setUserDetailsAndJoinRoles(result, userDTO, params);

		return userDTO;
	}

	private void setUserDetailsAndJoinRoles(Map result, UserDTO userDTO, Map<String, String> params) {
		if (null != result) {

			// Get user information
			Map user = (Map) result.get("user");
			userDTO.setFirstName(user.get("first-name").toString());
			userDTO.setLastName(user.get("last-name").toString());
			userDTO.setEmail(user.get("email").toString());
			userDTO.setLangKey(user.get("key").toString());
			userDTO.setLogin(user.get("name").toString());
			userDTO.setDisplayName(user.get("display-name").toString());
			userDTO.setActive(Boolean.parseBoolean(user.get("active").toString()));

			// Get all roles of user
			params.clear();
			params.put("username", user.get("name").toString());
			result = restTemplate.getForObject("/user/group/direct?username={username}", Map.class, params);
			if (null != result) {
				ArrayList<?> arrRoles = (ArrayList<?>) result.get("groups");
				List<String> lstRoles = new ArrayList<>();
                for (Object arrRole : arrRoles) {
                    Map role = (Map) arrRole;
                    lstRoles.add(AuthoritiesConstants.ROLE_PREFIX + role.get("name"));
                }
				userDTO.setRoles(lstRoles);
			}
		}
	}

    public List<UserDTO> searchUsersByGroup(String groupname, String username, int maxResults, int startAt) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupname", groupname);
        params.put("maxResults", maxResults);
        params.put("startIndex", startAt);
        if (StringUtils.hasLength(username)) {
            params.put("username", username);
        }

        Map response = restTemplate.getForObject("/group/user/direct?groupname={groupname}&max-results={maxResults}&start-index={startIndex}" + (StringUtils.hasLength(username) ? "&username={username}" : ""), Map.class, params);
        List<UserDTO> result = new ArrayList<>();
        if (response != null) {
			if (response.containsKey("users")) {
				ArrayList<?> userArr = (ArrayList<?>) response.get("users");
				for (Object o : userArr) {
					Map user = (Map) o;
					UserDTO userDTO = getUser(user.get("name").toString());
					userDTO.setEmail(null);

					result.add(userDTO);
				}
			} else if (response.containsKey("name")) {
				UserDTO userDTO = getUser(response.get("name").toString());
				userDTO.setEmail(null);
				result.add(userDTO);
			}
        }

        return result;
    }

	@CacheEvict(value = "accountCache", key = "#token")
	public void invalidateToken(String token) {
		restTemplate.delete("/session/{token}", token);	
	}
}
