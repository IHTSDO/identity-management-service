package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class IdentityProviderFactory {

	private final RestTemplate restTemplate;
	private final ProviderType providerType;
	private final String fileDirectory;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public enum ProviderType {
		CROWD, FILE
	}

	public IdentityProviderFactory(@Autowired RestTemplate restTemplate, @Value("${identity-provider}") ProviderType providerType,
			@Value("${identity-provider.file.directory}") String fileDirectory) {
		this.restTemplate = restTemplate;
		this.providerType = providerType;
		this.fileDirectory = fileDirectory.strip();
	}

	public IdentityProvider getIdentityProvider() throws IOException {
		logger.info("Using identity provider type {}", providerType);
		return switch (providerType) {
			case CROWD -> new CrowdRestClient(restTemplate);
			case FILE -> new PropertyFileIdentityProvider(fileDirectory);
		};
	}

}
