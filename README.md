# IHTSDO Tools Identity Management & SSO Service

# Modules

### Identity Service
This module is the IMS API written in Spring Boot. It provides single sign-on authentication and authorisation functionality, backed by Atlasian Crowd. User groups (labeled as roles) are cached to provide a faster lookup.

The Swagger interface for the production deployment is here: https://ims.ihtsdotools.org/swagger-ui.html

### Identity UI
This module is the user login and landing page screens written in Angular.

## How to enable SSO in an IHTSDO tools application
See the [IHTSDO Spring SSO project](https://github.com/IHTSDO/ihtsdo-spring-sso).

## Crowd configuration
See [Crowd Trusted Proxy settings](https://confluence.atlassian.com/display/CROWD/Debugging+SSO+in+environments+with+Proxy+Servers).
	
## FE only Local Deployment
Check out the repository and navigate to the identity-service folder. From here run the following: 

Run `npm install grunt -g` to make the local application aware of your grunt installation.ß
Run `npm update` to ensure grunt dependencies are up to date.

Run `bower install` to catch all build dependencies.

Run `grunt build` to populate the dist folder.

Add the following to your nginx http block (must run on port 8080 unless you change the port proxyed to within the grunt serve script):
```
server {
		listen		8080;
		server_name	localhost;
 
		location / {
            root /Users/chrisswires/identity-management/identity-service/src/main/webapp/dist;
		}
        location /i18n {
            alias /Users/chrisswires/identity-management/identity-service/src/main/webapp/i18;
		}
        location /api {
			proxy_pass https://dev-ims.ihtsdotools.org/api;
		}
	}
```

Run grunt serve. 

In order to see changes made to styles (for the FE changes should be made within identity-service/src/main/webapp) you will need to re-run grunt build and then grunt serve. 

In order to make use of the back-end functionality (log-in out etc) you will need to access the local application via http://local.ihtsdotools.org:8080. This allows IMS to set cookies against your local deployment. 
