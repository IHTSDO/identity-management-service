package org.snomed.ims.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	private final ApplicationProperties applicationProperties;

	public RestTemplateConfig(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder()
				.rootUri(applicationProperties.getCrowdApiUrl())
				.basicAuthentication(applicationProperties.getCrowdApiAppName(), applicationProperties.getCrowdApiAppPassword())
				.build();
	}
}
