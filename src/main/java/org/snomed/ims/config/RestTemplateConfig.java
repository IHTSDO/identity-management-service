package org.snomed.ims.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateConfig.class);
	
	private final ApplicationProperties applicationProperties;

	public RestTemplateConfig(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@Bean(name = "crowd")
	public RestTemplate crowdRestTemplate() {
		LOGGER.info("Creating Crowd RestTemplate with rootUri: {}", applicationProperties.getCrowdApiUrl());
		return new RestTemplateBuilder()
				.rootUri(applicationProperties.getCrowdApiUrl())
				.basicAuthentication(applicationProperties.getCrowdApiAppName(), applicationProperties.getCrowdApiAppPassword())
				.build();
	}

	@Bean(name = "keycloak")
	public RestTemplate keyCloakRestTemplate() {
		LOGGER.info("Creating Keycloak RestTemplate with rootUri: {}", applicationProperties.getKeycloakUrl());
		return new RestTemplateBuilder()
				.rootUri(applicationProperties.getKeycloakUrl())
				.build();
	}
}
