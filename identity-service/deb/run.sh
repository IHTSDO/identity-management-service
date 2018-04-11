#!/bin/bash
exec /usr/bin/java -Xms128m -Xmx512m -DcrowdPropertiesFileLocation=/opt/identity-service/lib/WEB-INF/classes/ org.springframework.boot.loader.WarLauncher --spring.profiles.active=prod
