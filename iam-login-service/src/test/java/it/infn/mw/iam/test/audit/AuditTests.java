package it.infn.mw.iam.test.audit;

import static it.infn.mw.iam.audit.IamAuditField.category;
import static it.infn.mw.iam.audit.IamAuditField.failureType;
import static it.infn.mw.iam.audit.IamAuditField.generatedBy;
import static it.infn.mw.iam.audit.IamAuditField.target;
import static it.infn.mw.iam.audit.IamAuditField.type;
import static it.infn.mw.iam.audit.IamAuditUtils.AUTHN_CATEGORY;
import static it.infn.mw.iam.audit.IamAuditUtils.AUTHZ_CATEGORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.event.AuthenticationCredentialsNotFoundEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.audit.IamAuthenticationAuditListener;
import it.infn.mw.iam.audit.IamAuthorizationAuditListener;
import it.infn.mw.iam.core.IamUserDetailsService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class AuditTests {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private IamUserDetailsService userDetailService;

  @Autowired
  private IamAuthenticationAuditListener authenticationListener;

  @Autowired
  private IamAuthorizationAuditListener authorizationListener;

  private Map<String, Object> data;

  @Test
  @WithMockUser(username = "admin", password = "bad_password")
  public void testAuthenticationFailureBadCredentialsEvent() throws Exception {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    BadCredentialsException e = new BadCredentialsException("Bad credentials test");

    eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(auth, e));

    data = authenticationListener.getAuditData();
    assertNotNull(data);
    assertEquals(data.get(category), AUTHN_CATEGORY);
    assertEquals(data.get(type), AuthenticationFailureBadCredentialsEvent.class.getSimpleName());
    assertEquals(data.get(failureType), BadCredentialsException.class.getSimpleName());
  }

  @Test
  @WithMockUser(username = "admin", password = "password")
  public void testAuthenticationSwitchUserEvent() throws Exception {
    String targetUsername = "test";

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserDetails targetUser = userDetailService.loadUserByUsername(targetUsername);

    eventPublisher.publishEvent(new AuthenticationSwitchUserEvent(auth, targetUser));

    data = authenticationListener.getAuditData();
    assertNotNull(data);
    assertEquals(data.get(category), AUTHN_CATEGORY);
    assertEquals(data.get(type), AuthenticationSwitchUserEvent.class.getSimpleName());
    assertEquals(data.get(target), targetUsername);
  }

  @Test
  @WithMockUser(username = "admin", password = "password")
  public void testInteractiveAuthenticationSuccessEvent() throws Exception {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(auth, this.getClass()));

    data = authenticationListener.getAuditData();
    assertNotNull(data);
    assertEquals(data.get(category), AUTHN_CATEGORY);
    assertEquals(data.get(type), InteractiveAuthenticationSuccessEvent.class.getSimpleName());
    assertEquals(data.get(generatedBy), this.getClass().getSimpleName());
  }

  @Test
  public void testAuthenticationCredentialsNotFoundEvent() throws Exception {

    AuthenticationCredentialsNotFoundEvent event = new AuthenticationCredentialsNotFoundEvent(this,
        Collections.<ConfigAttribute>singletonList(new SecurityConfig("USER")),
        new AuthenticationCredentialsNotFoundException("Bad user"));

    eventPublisher.publishEvent(event);

    data = authorizationListener.getAuditData();
    assertNotNull(data);
    assertEquals(data.get(category), AUTHZ_CATEGORY);
    assertEquals(data.get(type), AuthenticationCredentialsNotFoundEvent.class.getSimpleName());
    assertEquals(data.get(failureType),
        AuthenticationCredentialsNotFoundException.class.getSimpleName());
  }

  @Test
  public void testAuthorizationFailureEvent() throws Exception {

    AuthorizationFailureEvent event = new AuthorizationFailureEvent(this,
        Collections.<ConfigAttribute>singletonList(new SecurityConfig("USER")),
        new UsernamePasswordAuthenticationToken("user", "password"),
        new AccessDeniedException("Bad user"));

    eventPublisher.publishEvent(event);

    data = authorizationListener.getAuditData();
    assertNotNull(data);
    assertEquals(data.get(category), AUTHZ_CATEGORY);
    assertEquals(data.get(type), AuthorizationFailureEvent.class.getSimpleName());
    assertEquals(data.get(failureType), AccessDeniedException.class.getSimpleName());
  }


}
