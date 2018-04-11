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
package org.ihtsdo.otf.im.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**IHTSDO Tools role. It can be used to obtain assigned role to user 
 * and role's description if available in crowd server.
 *
 */
public class IHTSDOToolsAuthority implements GrantedAuthority {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String role;
	
	private String description;
	
	
    public IHTSDOToolsAuthority(String role) {
    	
        Assert.hasText(role, "Role is mandatory for an IHTSDO Authority");
        this.role = role;
    }

	/* (non-Javadoc)
	 * @see org.springframework.security.core.GrantedAuthority#getAuthority()
	 */
	@Override
	public String getAuthority() {

		return role;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj instanceof IHTSDOToolsAuthority) {
        	
            return role.equals(((IHTSDOToolsAuthority) obj).role);
            
        }

        return false;
    
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		return this.role.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("Role : %s and Role Description : %s", this.role, this.description);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}


}
