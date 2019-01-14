package org.ihtsdo.otf.im.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger configuration.
 * <p>
 * Warning! When having a lot of REST endpoints, Swagger can become a performance issue. In that
 * case, you can use a specific Spring profile for this class, so that only front-end developers
 * have access to the Swagger view.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

	 @Bean
	    public Docket api() {
	        return new Docket(DocumentationType.SWAGGER_2).select()
	            .apis(RequestHandlerSelectors
	                .basePackage("org.ihtsdo.otf.im.rest"))
	            .paths(PathSelectors.regex("/.*"))
	            .build().apiInfo(apiEndPointsInfo());
	    }
	    private ApiInfo apiEndPointsInfo() {
	        return new ApiInfoBuilder().title("Spring Boot REST API")
	            .description("Identity Management REST API")
	            .license("Apache 2.0")
	            .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
	            .version("2.0.0")
	            .build();
	    }
}
