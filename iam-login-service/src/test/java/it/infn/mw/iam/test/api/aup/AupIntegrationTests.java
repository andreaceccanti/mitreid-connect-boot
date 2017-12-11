package it.infn.mw.iam.test.api.aup;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.aup.model.AupConverter;
import it.infn.mw.iam.api.aup.model.AupDTO;
import it.infn.mw.iam.persistence.model.IamAup;
import it.infn.mw.iam.persistence.repository.IamAupRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithAnonymousUser
public class AupIntegrationTests extends AupTestSupport {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private IamAupRepository aupRepo;

  @Autowired
  private AupConverter converter;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;
  
  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .alwaysDo(print())
      .apply(springSecurity())
      .build();
  }
  
  @After
  public void cleanupOAuthUser() {
    mockOAuth2Filter.cleanupSecurityContext();
  }


  @Test
  public void noAupDefinedResultsin404() throws Exception {
    mvc.perform(get("/iam/aup")).andExpect(status().isNotFound());
  }

  @Test
  public void aupIsReturnedIfDefined() throws Exception {

    IamAup defaultAup = buildDefaultAup();
    aupRepo.save(defaultAup);

    mvc.perform(get("/iam/aup")).andExpect(status().isOk());

  }

  @Test
  public void aupCreationRequiresAuthenticatedUser() throws JsonProcessingException, Exception {
    Date now = new Date();
    AupDTO aup = new AupDTO("text", "desc", -1L, now, now);

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isUnauthorized());

  }

  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void aupCreationRequiresAdminPrivileges() throws JsonProcessingException, Exception {
    Date now = new Date();
    AupDTO aup = new AupDTO("text", "desc", -1L, now, now);

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isForbidden());

  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupTextIsRequired() throws JsonProcessingException, Exception {
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());

    aup.setText(null);

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("Invalid AUP: the AUP text cannot be blank")));

  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupDescriptionNoLongerThan128Chars() throws JsonProcessingException, Exception {
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());
    String longDescription = Strings.repeat("xxxx", 33);
    aup.setDescription(longDescription);

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error",
          equalTo("Invalid AUP: the description string must be at most 128 characters long")));

  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupCreationWorks() throws JsonProcessingException, Exception {
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());


    String aupJson = mvc.perform(get("/iam/aup"))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    AupDTO createdAup = mapper.readValue(aupJson, AupDTO.class);

    assertThat(createdAup.getText(), equalTo(aup.getText()));
    assertThat(createdAup.getDescription(), equalTo(aup.getDescription()));
    assertThat(createdAup.getSignatureValidityInDays(), equalTo(aup.getSignatureValidityInDays()));
    assertThat(createdAup.getCreationTime(), greaterThan(aup.getCreationTime()));
    assertThat(createdAup.getLastUpdateTime(), greaterThan(aup.getLastUpdateTime()));
  }
  
  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupCreationFailsIfAupAlreadyDefined() throws JsonProcessingException, Exception {
    
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());
    
    mvc
    .perform(
        post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
    .andExpect(status().isConflict())
    .andExpect(jsonPath("$.error", equalTo("AUP already exists")));    
  }
  
  @Test
  public void aupDeletionRequiresAuthenticatedUser() throws Exception {
    mvc.perform(delete("/iam/aup")).andExpect(status().isUnauthorized());
  }
  
  @Test
  @WithMockUser(username = "test", roles = {"USER"})
  public void aupDeletionRequiresAdminUser() throws Exception {
    mvc.perform(delete("/iam/aup")).andExpect(status().isForbidden());
  }
  
  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupDeletionReturns404IfAupIsNotDefined() throws Exception {
    mvc.perform(delete("/iam/aup")).andExpect(status().isNotFound());
  }
  
  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  public void aupDeletionWorks() throws Exception {
    AupDTO aup = converter.dtoFromEntity(buildDefaultAup());

    mvc
      .perform(
          post("/iam/aup").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(aup)))
      .andExpect(status().isCreated());
    
    mvc.perform(delete("/iam/aup")).andExpect(status().isNoContent());
    
    mvc.perform(get("/iam/aup")).andExpect(status().isNotFound());
  }

}
