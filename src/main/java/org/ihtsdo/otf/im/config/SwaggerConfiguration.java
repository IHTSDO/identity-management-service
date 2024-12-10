package org.ihtsdo.otf.im.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Swagger configuration.
 * <p>
 * Warning! When having a lot of REST endpoints, Swagger can become a performance issue. In that
 * case, you can use a specific Spring profile for this class, so that only front-end developers
 * have access to the Swagger view.
 */
@Configuration
public class SwaggerConfiguration {
	@Autowired(required = false)
	private BuildProperties buildProperties;

	@Bean
	public GroupedOpenApi apiDocs() {
		return GroupedOpenApi.builder()
				.group("identity-service")
				.pathsToExclude("/error", "/") // Don't show the error or root endpoints in Swagger
				.build();
	}

	@Bean
	public OpenAPI apiInfo() {
		final String version = buildProperties != null ? buildProperties.getVersion() : "DEV";
		return new OpenAPI()
				.info(new Info()
						.title("Identity Service")
						.description("Microservice to ensure service acceptance criteria are met before content promotion within the SNOMED CT Authoring Platform.")
						.version(version)
						.contact(new Contact().name("SNOMED International").url("https://www.snomed.org"))
						.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("See more about Identity Management Service in GitHub")
						.url("https://github.com/IHTSDO/identity-management-service"));
	}
}
