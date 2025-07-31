# Identity Management Service

Identity Management Service is a Spring Boot web API providing single sign-on authentication and authorisation.
The user data store is configurable. Options are Atlassian Crowd (default) or basic file-based store.

### Modules
- See [here](https://github.com/IHTSDO/identity-management-ui) for identity-management-ui, a supporting web app.

### How to
- See [here](https://github.com/IHTSDO/ihtsdo-spring-sso) for an example on how to enable SSO in a SNOMED International developed application.
- See [here](https://confluence.atlassian.com/display/CROWD/Debugging+SSO+in+environments+with+Proxy+Servers) on how to configure Atlassian Crowd.

#### File-Based User Store
Basic implementations with a small number of users can opt to manage users in a basic file-based store as an alternative to Atlassian Crowd.

To enable file-based store set configuration item `identity-provider=FILE`.

- Files
  - `users.txt`
    - This file contains a list of users and their passwords. The format is `username=password`.
  - `user-groups.txt`
    - This file contains a list of users and their groups. The format is `username=group1,group2,group3`.
  - Files should be stored in the same directory as the jar file.
  - Copy example directory `example_file_based_user_store` to `user_store` then set `identity-provider.file.directory=user_store`.

---
