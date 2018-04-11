IHTSDO Tools Identity Management & SSO Service
===============================================

This service includes an identity-service module , a IHTSDO-Crowd bridge module and IHTSDO-Crowd resource module.

identity-service is an authentication module deployed as a federated service and has its own angularjs UI

ihtsdo-crowd-bridge module which works as a bridge between Atlassian Crowd & authentication module.

ihtsdo-crowd-resource module contains all the resources required to enable SSO in an IHTSDO tools application

Build
========

Production build can be done using following command

`mvn -Pprod clean package`

for UAT
`mvn -Puat clean package`


Above generate two wars of identity-service module beside other artifacts.

    ~/identity-service/target/identity-service-0.0.1-SNAPSHOT.war
    ~/identity-service/target/identity-service-0.0.1-SNAPSHOT.war.original

From above two wars *war.original should be deployed or used as part of debian package as it does not includes tomcat binaries. First one is executable war.

In development application can be run using 

`mvn spring-boot:run`

Deployment
==========
Application uses yml based properties configuration and based on each deployment environment, application start script must pass  spring.profiles.active property value as java option. 

For example in development environment it is `-Dspring.profiles.active=dev`

in  user acceptance test environment `-Dspring.profiles.active=uat`

and in production it is `-Dspring.profiles.active=prod`


How to enable SSO in an IHTSDO tools application
================================================

See on [confluence](https://confluence.ihtsdotools.org/pages/viewpage.action?pageId=7537010)

Crowd configuration rules
=========================
See on [confluence](https://confluence.ihtsdotools.org/display/IMS/IHTSDO+Tools+Security+Policies)
[Please also see a note Trusted Proxy settings](https://confluence.atlassian.com/display/CROWD/Debugging+SSO+in+environments+with+Proxy+Servers)
	
FE only Local Deployment
=========================

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
