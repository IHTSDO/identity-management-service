package org.ihtsdo.otf.im.web.rest;

import org.ihtsdo.otf.im.Application;
import org.ihtsdo.otf.im.sso.service.IHTSDOUserDetailsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.inject.Inject;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class UserResourceTest {

	@Inject
	private IHTSDOUserDetailsService userRepository;

	private MockMvc restUserMockMvc;

	@Before
	public void setup() {
		UserResource userResource = new UserResource();
		ReflectionTestUtils.setField(userResource, "userRepository", userRepository);
		this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource).build();
	}

    /*@Test
	public void testGetExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/admin")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.lastName").value("Administrator"));
    }

    @Test
    public void testGetUnknownUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/unknown")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }*/

	@Test
	public void testGetKnownUser() throws Exception {

	}
}
