package it.infn.mw.iam.test.oauth;



import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebAppConfiguration
@Transactional
public class IntrospectionEndpointTests extends EndpointsTestUtils {

  @Value("${iam.organisation.name}")
  String organisationName;
  
  @Value("${iam.issuer}")
  String issuer;

  private static final String ENDPOINT = "/introspect";
  private static final String CLIENT_ID = "password-grant";
  private static final String CLIENT_SECRET = "secret";

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(print())
      .build();
  }

  @Test
  public void testIntrospectionEndpointRetursBasicUserInformation() throws Exception {
    String accessToken = getPasswordAccessToken();

    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)))
      .andExpect(jsonPath("$.iss", equalTo(issuer+"/")))
      .andExpect(jsonPath("$.groups", hasSize(equalTo(2))))
      .andExpect(jsonPath("$.groups", containsInAnyOrder("Production", "Analysis")))
      .andExpect(jsonPath("$.preferred_username", equalTo("test")))
      .andExpect(jsonPath("$.organisation_name", equalTo(organisationName)))
      .andExpect(jsonPath("$.email", equalTo("test@iam.test")));
    // @formatter:on
  }

  @Test
  public void testNoGroupsReturnedWithoutProfileScope() throws Exception {
    String accessToken = getPasswordAccessToken("openid");

    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)))
      .andExpect(jsonPath("$.groups").doesNotExist())
      .andExpect(jsonPath("$.preferred_username").doesNotExist())
      .andExpect(jsonPath("$.organisation_name").doesNotExist())
      .andExpect(jsonPath("$.email").doesNotExist());
    // @formatter:on
  }

  @Test
  public void testEmailReturnedWithEmailScope() throws Exception {
    String accessToken = getPasswordAccessToken("openid email");

    // @formatter:off
    mvc.perform(post(ENDPOINT)
        .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
        .param("token", accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.active", equalTo(true)))
      .andExpect(jsonPath("$.groups").doesNotExist())
      .andExpect(jsonPath("$.preferred_username").doesNotExist())
      .andExpect(jsonPath("$.organisation_name").doesNotExist())
      .andExpect(jsonPath("$.email", equalTo("test@iam.test")));
    // @formatter:on
  }
}
