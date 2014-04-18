package org.karmaexchange.provider;

import static java.lang.String.format;
import static org.karmaexchange.util.Properties.Property.FACEBOOK_APP_SECRET;
import static org.karmaexchange.util.ServletUtil.getRequestUri;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import lombok.Data;

import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.AdminTaskServlet;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.Properties;
import org.karmaexchange.util.ServletUtil;

import com.restfb.DefaultFacebookClient;
import com.restfb.Facebook;
import com.restfb.exception.FacebookException;

// TODO(avaliani): This class is now out of sync. It's missing age_range, gender
@SuppressWarnings("serial")
public class FacebookRegistrationServlet extends AdminTaskServlet {

  private static final Logger logger =
      Logger.getLogger(FacebookRegistrationServlet.class.getName());
  public static final Level REGISTRATION_LOG_LEVEL = Level.FINE;

  private static final String SIGNED_REQUEST_PARAM = "signed_request";

  private static final String EXPECTED_FIELDS_METADATA =
      "[{'name':'name'},{'name':'first_name'},{'name':'last_name'},{'name':'email'}," +
      "{'name':'location'},{'name':'captcha'}]";

  public FacebookRegistrationServlet() {
    super(AdminUtil.AdminTaskType.REGISTRATION);
  }

  @Override
  public void execute() throws IOException {
    try {
      String signedRequestStr = req.getParameter(SIGNED_REQUEST_PARAM);
      if (signedRequestStr == null) {
        throw ErrorResponseMsg.createException("signed request missing",
          ErrorInfo.Type.BAD_REQUEST);
      }
      String appSecret = Properties.get(getRequestUri(req), FACEBOOK_APP_SECRET);

      SignedRegistrationRequest registrationReq;
      try {
        registrationReq = new DefaultFacebookClient().parseSignedRequest(
          signedRequestStr, appSecret, SignedRegistrationRequest.class);
      } catch (FacebookException e) {
        throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
      }
      registrationReq.validate();

      User.persistNewUser(
        registrationReq.createUser());

      resp.sendRedirect("/");
    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      logger.log(REGISTRATION_LOG_LEVEL, "Failed to register user:\n  " + errMsg.getEntity());
      ServletUtil.setResponse(resp, e);
      // TODO(avaliani): If registration fails it would be good to have a redirect page to
      // paste the error to.
    }
  }

  @Data
  private static class SignedRegistrationRequest {
    @Facebook("oauth_token")
    private String oAuthToken;

    @Facebook("registration")
    private RegistrationInfo registrationInfo;

    @Facebook("registration_metadata")
    private RegistrationMetadata registrationMetadata;

    @Facebook("user_id")
    private String userId;

    public void validate() {
      checkFieldNotNull(oAuthToken, "oAuthToken");
      checkFieldNotNull(registrationInfo, "registrationInfo");
      checkFieldNotNull(registrationInfo.name, "registrationInfo.name");
      checkFieldNotNull(registrationInfo.firstName, "registrationInfo.firstName");
      // skip last name - it's okay for someone not to specify this
      // skip email - it's okay for someone not to specify this
      // skip location - it's okay for someone not to specify this
      checkFieldNotNull(registrationMetadata, "registrationMetadata");
      checkFieldNotNull(registrationMetadata.fields, "registrationMetadata.fields");
      checkFieldNotNull(userId, "userId");

      // Handle fields meta data attack mentioned in:
      //   https://developers.facebook.com/docs/plugins/registration/advanced/
      String metadataNoWs = registrationMetadata.fields.replaceAll("\\s", "");
      if (!metadataNoWs.equals(EXPECTED_FIELDS_METADATA)) {
        throw ErrorResponseMsg.createException(
          "registration form field mis-match: '" + metadataNoWs + "'",
          ErrorInfo.Type.BAD_REQUEST);
      }
    }

    private void checkFieldNotNull(Object value, String fieldName) {
      if (value == null) {
        throw ErrorResponseMsg.createException(
          format("bad registration request: %s is null", fieldName),
          ErrorInfo.Type.BAD_REQUEST);
      }
    }

    private User createUser() {
      OAuthCredential credential = OAuthCredential.create(
        SocialNetworkProviderType.FACEBOOK.getOAuthProviderName(), userId, oAuthToken);
      FacebookSocialNetworkProvider.verifyCredential(credential);

      User user = User.create(credential);
      user.setFirstName(registrationInfo.firstName);
      user.setLastName(registrationInfo.lastName);
      if (registrationInfo.email != null) {
        user.getRegisteredEmails().add(new RegisteredEmail(registrationInfo.email, true));
      }
      user.setAddress(parseCity());
      return user;
    }

    private Address parseCity() {
      if ((registrationInfo.currentCity == null) || (registrationInfo.currentCity.name == null)) {
        return null;
      }
      return FacebookSocialNetworkProvider.parseCity(registrationInfo.currentCity.name);
    }

    @Data
    private static class RegistrationInfo {

      @Facebook
      private String name;

      @Facebook("first_name")
      private String firstName;

      @Facebook("last_name")
      private String lastName;

      @Facebook
      private String email;

      @Facebook("location")
      private NameAndIdFacebookType currentCity;
    }

    @Data
    private static class RegistrationMetadata {
      @Facebook
      private String fields;

    }
  }

  // TODO(avaliani): Verify if NamedFacebookType can safely replace it. Most likely it can.
  @Data
  private static class NameAndIdFacebookType {

    @Facebook
    private String name;

    @Facebook
    private Long id;
  }
}
