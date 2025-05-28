package org.snomed.ims;

import org.snomed.ims.service.IdentityProvider;
import org.snomed.ims.service.IdentityProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class Application {

	@Bean
	public IdentityProvider getIdentityProvider(@Autowired IdentityProviderFactory factory) throws IOException {
		return factory.getIdentityProvider();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
