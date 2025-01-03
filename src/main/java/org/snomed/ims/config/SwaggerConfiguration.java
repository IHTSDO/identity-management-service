package org.snomed.ims.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfiguration implements WebMvcConfigurer {
	private final ApplicationProperties applicationProperties;

	public SwaggerConfiguration(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@Bean
	public GroupedOpenApi apiDocs() {
		return GroupedOpenApi.builder()
				.group(applicationProperties.getProjectName())
				.pathsToExclude("/error", "/") // Don't show the error or root endpoints in Swagger
				.build();
	}

	@Bean
	public OpenAPI apiInfo() {
		return new OpenAPI()
				.info(new Info()
						.title(applicationProperties.getProjectName())
						.description(applicationProperties.getProjectDescription())
						.version(applicationProperties.getProjectVersion())
						.contact(new Contact().name("SNOMED International").url("https://www.snomed.org"))
						.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
				.externalDocs(new ExternalDocumentation()
						.description("See more about Identity Management Service in GitHub")
						.url("https://github.com/IHTSDO/identity-management-service"));
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// Redirect root endpoint
		registry.addRedirectViewController("/", "/swagger-ui/index.html");
	}
}
