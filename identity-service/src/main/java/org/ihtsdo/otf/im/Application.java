package org.ihtsdo.otf.im;

import org.ihtsdo.otf.im.service.CrowdRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableWebSecurity
public class Application implements ApplicationRunner {

	public static final String TEST_USERNAME = "test-username";
	public static final String TEST_PASSWORD = "test-password";
	@Autowired
	private CrowdRestClient crowdRestClient;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		if (applicationArguments.containsOption(TEST_USERNAME) && applicationArguments.containsOption(TEST_PASSWORD)) {
			String username = applicationArguments.getOptionValues("username").get(0);
			String password = applicationArguments.getOptionValues("password").get(0);
			crowdRestClient.authenticate(username, password);
		}
	}
}
