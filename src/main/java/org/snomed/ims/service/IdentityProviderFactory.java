package org.snomed.ims.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.ims.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class IdentityProviderFactory {

    private final RestTemplate crowdRestTemplate;
    private final RestTemplate keyCloakRestTemplate;
    private final ProviderType providerType;
    private final String fileDirectory;

    private final ApplicationProperties applicationProperties;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public enum ProviderType {
        CROWD, FILE, KEYCLOAK
    }

    public IdentityProviderFactory(@Qualifier("crowd") @Autowired RestTemplate crowdRestTemplate, @Qualifier("keycloak") @Autowired RestTemplate keyCloakRestTemplate, @Value("${identity-provider}") ProviderType providerType,
                                   @Value("${identity-provider.file.directory}") String fileDirectory, ApplicationProperties applicationProperties) {
        this.crowdRestTemplate = crowdRestTemplate;
        this.keyCloakRestTemplate = keyCloakRestTemplate;
        this.providerType = providerType;
        this.fileDirectory = fileDirectory.strip();
        this.applicationProperties = applicationProperties;
    }

    public IdentityProvider getIdentityProvider() throws IOException {
        logger.info("Using identity provider type {}", providerType);
        return switch (providerType) {
            case CROWD -> new CrowdRestClient(crowdRestTemplate);
            case FILE -> new PropertyFileIdentityProvider(fileDirectory);
            case KEYCLOAK -> new KeyCloakIdentityProvider(keyCloakRestTemplate, applicationProperties.getKeycloakUrl(), applicationProperties.getKeycloakRealms(), applicationProperties.getKeycloakClientId(), applicationProperties.getKeycloakClientSecrete(), applicationProperties.getKeycloakAdminClientId(), applicationProperties.getKeycloakAdminClientSecret());
        };
    }

}
