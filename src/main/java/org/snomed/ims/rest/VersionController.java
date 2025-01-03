package org.snomed.ims.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.ims.config.ApplicationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "VersionController")
public class VersionController {
	private final ApplicationProperties applicationProperties;

	public VersionController(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@GetMapping(value = "/version", produces = "application/json")
	public Map<String, String> getVersion() {
		String projectVersion = applicationProperties.getProjectVersion();
		String time = Instant.now().toString();

		return Map.of("version", projectVersion, "time", time);
	}
}
