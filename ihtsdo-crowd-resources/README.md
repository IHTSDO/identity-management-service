IHTSDO Tools Identity Management & SSO Service : IHTSDO Crowd Bridge
==========================

This project includes common files to enable Crowd SSO in IHTSDO tools

files available in src/main/resources should go inside WEB-INF/classes i.e. should be available on application classpath.
Depending on application configuration spring beans provided in applicationContext-CrowdClient.xml, application-im-common-security-config.xml configuration files must be loaded before their use. These can be loaded by configuring a spring context listener in web.xml or using appropriate annotations.  

