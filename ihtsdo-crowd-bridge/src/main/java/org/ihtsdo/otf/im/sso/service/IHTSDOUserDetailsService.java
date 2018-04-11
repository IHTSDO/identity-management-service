/**
* Copyright 2014 IHTSDO
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.ihtsdo.otf.im.sso.service;

import org.ihtsdo.otf.im.domain.IHTSDOUser;
import org.ihtsdo.otf.im.error.UserNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetails;
import com.atlassian.crowd.integration.springsecurity.user.CrowdUserDetailsServiceImpl;


/**
 *IHTSDO User Details service
 */
public class IHTSDOUserDetailsService extends CrowdUserDetailsServiceImpl {
	

	//TODO some more services are required like getting application names etc hence customization. will be done later
	public IHTSDOUser getUserByUserName(String userName)
			throws UserNotFoundException {
		
		if (StringUtils.isEmpty(userName)) {
			
			throw new UserNotFoundException("Invalid username. It can not be empty or null");
		}
		
		try {
			
			CrowdUserDetails ssoUser = super.loadUserByUsername(userName);
			IHTSDOUser user = IHTSDOUser.getInstance(ssoUser);
			return user;

			
		} catch (UsernameNotFoundException e) {
			
			throw new UserNotFoundException(e.getMessage());
		}
		
	}
	

}
