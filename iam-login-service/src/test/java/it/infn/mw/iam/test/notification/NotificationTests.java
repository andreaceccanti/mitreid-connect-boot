package it.infn.mw.iam.test.notification;

import static it.infn.mw.iam.test.RegistrationUtils.approveRequest;
import static it.infn.mw.iam.test.RegistrationUtils.confirmRegistrationRequest;
import static it.infn.mw.iam.test.RegistrationUtils.createRegistrationRequest;
import static it.infn.mw.iam.test.RegistrationUtils.deleteUser;
import static it.infn.mw.iam.test.RegistrationUtils.rejectRequest;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.account.password_reset.PasswordResetController;
import it.infn.mw.iam.api.account.password_reset.PasswordResetService;
import it.infn.mw.iam.core.IamDeliveryStatus;
import it.infn.mw.iam.notification.MockTimeProvider;
import it.infn.mw.iam.notification.NotificationProperties;
import it.infn.mw.iam.notification.NotificationService;
import it.infn.mw.iam.persistence.model.IamEmailNotification;
import it.infn.mw.iam.persistence.repository.IamEmailNotificationRepository;
import it.infn.mw.iam.registration.PersistentUUIDTokenGenerator;
import it.infn.mw.iam.registration.RegistrationRequestDto;
import it.infn.mw.iam.test.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@WebIntegrationTest
public class NotificationTests {

  @Autowired
  @Qualifier("defaultNotificationService")
  private NotificationService notificationService;

  @Autowired
  private NotificationProperties properties;

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;

  @Value("${iam.organisation.name}")
  private String organisationName;

  @Value("${iam.baseUrl}")
  private String baseUrl;

  @Autowired
  private IamEmailNotificationRepository notificationRepository;

  @Autowired
  private PasswordResetService passwordResetService;

  @Autowired
  private MockTimeProvider timeProvider;

  @Autowired
  private PersistentUUIDTokenGenerator generator;

  private Wiser wiser;


  @BeforeClass
  public static void init() {

    TestUtils.initRestAssured();
  }

  @Before
  public void setUp() {
    wiser = new Wiser();
    wiser.setHostname(mailHost);
    wiser.setPort(mailPort);
    wiser.start();
  }

  @After
  public void tearDown() throws InterruptedException {
    wiser.stop();
    Thread.sleep(1000L);

    notificationRepository.deleteAll();
  }

  @Test
  public void testSendEmails() throws MessagingException {

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    notificationService.sendPendingNotifications();

    assertEquals(1, wiser.getMessages().size());
    WiserMessage message = wiser.getMessages().get(0);

    assertEquals(properties.getMailFrom(), message.getEnvelopeSender());
    assertTrue("receiver", message.getEnvelopeReceiver().startsWith(username));
    assertEquals(properties.getSubject().get("confirmation"),
        message.getMimeMessage().getSubject());

    Iterable<IamEmailNotification> queue = notificationRepository.findAll();
    for (IamEmailNotification elem : queue) {
      assertEquals(IamDeliveryStatus.DELIVERED, elem.getDeliveryStatus());
    }

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testDisableNotificationOption() {
    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);

    properties.setDisable(true);
    notificationService.sendPendingNotifications();

    assertEquals(0, wiser.getMessages().size());

    Iterable<IamEmailNotification> queue = notificationRepository.findAll();
    for (IamEmailNotification elem : queue) {
      assertEquals(IamDeliveryStatus.DELIVERED, elem.getDeliveryStatus());
    }

    deleteUser(reg.getAccountId());
    properties.setDisable(false);
  }

  @Test
  public void testSendMultipleNotifications() {

    int count = 3;
    List<RegistrationRequestDto> requestList = new ArrayList<>();

    for (int idx = 1; idx <= count; idx++) {
      RegistrationRequestDto reg = createRegistrationRequest("test_user_" + idx);
      requestList.add(reg);
    }

    notificationService.sendPendingNotifications();

    assertEquals(count, wiser.getMessages().size());

    Iterable<IamEmailNotification> queue = notificationRepository.findAll();
    for (IamEmailNotification elem : queue) {
      assertEquals(IamDeliveryStatus.DELIVERED, elem.getDeliveryStatus());
    }

    for (RegistrationRequestDto elem : requestList) {
      deleteUser(elem.getAccountId());
    }
  }

  @Test
  public void testSendWithEmptyQueue() {

    notificationService.sendPendingNotifications();
    assertEquals(0, wiser.getMessages().size());
  }

  @Test
  public void testDeliveryFailure() {
    String username = "test_user";
    RegistrationRequestDto reg = createRegistrationRequest(username);

    wiser.stop();

    notificationService.sendPendingNotifications();

    Iterable<IamEmailNotification> queue = notificationRepository.findAll();
    for (IamEmailNotification elem : queue) {
      assertEquals(IamDeliveryStatus.DELIVERY_ERROR, elem.getDeliveryStatus());
    }

    deleteUser(reg.getAccountId());
  }


  @Test
  public void testApproveFlowNotifications() throws MessagingException {
    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    notificationService.sendPendingNotifications();

    assertEquals(1, wiser.getMessages().size());

    WiserMessage message = wiser.getMessages().get(0);

    assertEquals(message.getMimeMessage().getSubject(),
        properties.getSubject().get("confirmation"));

    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);
    notificationService.sendPendingNotifications();

    assertEquals(2, wiser.getMessages().size());

    message = wiser.getMessages().get(1);

    assertEquals(properties.getSubject().get("adminHandleRequest"),
        message.getMimeMessage().getSubject());

    assertTrue("receiver", message.getEnvelopeReceiver().startsWith(properties.getAdminAddress()));

    approveRequest(reg.getUuid());
    notificationService.sendPendingNotifications();

    assertEquals(3, wiser.getMessages().size());

    message = wiser.getMessages().get(2);

    assertEquals(properties.getSubject().get("activated"), message.getMimeMessage().getSubject());

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testRejectFlowNotifications() throws MessagingException {
    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    notificationService.sendPendingNotifications();

    assertEquals(1, wiser.getMessages().size());

    WiserMessage message = wiser.getMessages().get(0);
    assertEquals(properties.getSubject().get("confirmation"),
        message.getMimeMessage().getSubject());

    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);
    notificationService.sendPendingNotifications();

    assertEquals(2, wiser.getMessages().size());
    message = wiser.getMessages().get(1);
    assertEquals(properties.getSubject().get("adminHandleRequest"),
        message.getMimeMessage().getSubject());
    assertTrue("receiver", message.getEnvelopeReceiver().startsWith(properties.getAdminAddress()));

    rejectRequest(reg.getUuid());
    notificationService.sendPendingNotifications();

    assertEquals(3, wiser.getMessages().size());

    message = wiser.getMessages().get(2);
    assertEquals(properties.getSubject().get("rejected"), message.getMimeMessage().getSubject());
  }

  @Test
  public void testCleanOldMessages() {
    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    notificationService.sendPendingNotifications();
    assertEquals(1, wiser.getMessages().size());

    Date fakeDate = DateUtils.addDays(new Date(), (properties.getCleanupAge() + 1));
    timeProvider.setTime(fakeDate.getTime());

    notificationService.clearExpiredNotifications();

    deleteUser(reg.getAccountId());

    int count = notificationRepository.countAllMessages();
    assertEquals(0, count);


  }

  @Test
  public void testEveryMailShouldContainSignature() throws MessagingException, IOException {
    String signature = String.format("The %s registration service", organisationName);

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);
    approveRequest(reg.getUuid());

    notificationService.sendPendingNotifications();

    for (WiserMessage message : wiser.getMessages()) {
      assertTrue("text/plain", message.getMimeMessage().isMimeType("text/plain"));
      String content = message.getMimeMessage().getContent().toString();
      assertThat(content, containsString(signature));
    }

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testConfirmMailShouldContainsConfirmationLink()
      throws MessagingException, IOException {

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    String confirmationKey = generator.getLastToken();

    String confirmURL = format("%s/registration/verify/%s", baseUrl, confirmationKey);

    notificationService.sendPendingNotifications();

    WiserMessage message = wiser.getMessages().get(0);

    assertTrue("text/plain", message.getMimeMessage().isMimeType("text/plain"));
    String content = message.getMimeMessage().getContent().toString();
    assertThat(content, containsString(confirmURL));

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testActivationMailShouldContainsResetPasswordLink()
      throws MessagingException, IOException {

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);
    approveRequest(reg.getUuid());
    String resetKey = generator.getLastToken();

    String resetPasswordUrl =
        format("%s%s/%s", baseUrl, PasswordResetController.BASE_TOKEN_URL, resetKey);

    notificationService.sendPendingNotifications();

    WiserMessage message = wiser.getMessages().get(2);

    assertTrue("text/plain", message.getMimeMessage().isMimeType("text/plain"));
    String content = message.getMimeMessage().getContent().toString();
    assertThat(content, containsString(resetPasswordUrl));

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testAdminNotificationMailShouldContainsDashboardLink()
      throws MessagingException, IOException {

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);

    String dashboardUrl = format("%s/dashboard#/requests", baseUrl);

    notificationService.sendPendingNotifications();

    WiserMessage message = wiser.getMessages().get(1);

    assertTrue("text/plain", message.getMimeMessage().isMimeType("text/plain"));
    String content = message.getMimeMessage().getContent().toString();
    assertThat(content, containsString(dashboardUrl));

    deleteUser(reg.getAccountId());
  }

  @Test
  public void testPasswordResetMailShouldContainsUsername() throws MessagingException, IOException {

    String username = "test_user";

    RegistrationRequestDto reg = createRegistrationRequest(username);
    String confirmationKey = generator.getLastToken();
    confirmRegistrationRequest(confirmationKey);
    approveRequest(reg.getUuid());
    passwordResetService.createPasswordResetToken(reg.getEmail());

    notificationService.sendPendingNotifications();

    List<WiserMessage> msgList = wiser.getMessages();
    WiserMessage message = wiser.getMessages().get(msgList.size() - 1);

    assertTrue("text/plain", message.getMimeMessage().isMimeType("text/plain"));
    String content = message.getMimeMessage().getContent().toString();
    assertThat(content, containsString(username));

    deleteUser(reg.getAccountId());
  }

}
