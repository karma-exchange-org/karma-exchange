package org.karmaexchange.provider;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.ContactInfo;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.AdminTaskServlet;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.ServletUtil;

import com.restfb.DefaultFacebookClient;

@SuppressWarnings("serial")
public class BootstrapFacebookRegistrationServlet extends AdminTaskServlet {

  private static final Logger logger =
      Logger.getLogger(BootstrapFacebookRegistrationServlet.class.getName());

  private static final String UID_PARAM = "uid";
  private static final String ACCESS_TOKEN_PARAM = "token";

  public BootstrapFacebookRegistrationServlet() {
    super(AdminUtil.AdminTaskType.BOOTSTRAP);
  }

  @Override
  public void execute() throws IOException {
    resp.setContentType("text/plain");
    PrintWriter statusWriter = resp.getWriter();
    try {
      statusWriter.println("About to bootstrap current user...");

      String uid = req.getParameter(UID_PARAM);
      String token = req.getParameter(ACCESS_TOKEN_PARAM);
      if ((uid == null) || (token == null)) {
        throw ErrorResponseMsg.createException("missing params", ErrorInfo.Type.BAD_REQUEST);
      }

      OAuthCredential credential = OAuthCredential.create(
        SocialNetworkProviderType.FACEBOOK.getOAuthProviderName(), uid, token);

      logger.log(Level.INFO, "[" + credential + "] Boostraping user");

      FacebookSocialNetworkProvider.verifyCredential(credential);

      FacebookRegistrationServlet.persistUser(createUser(credential));

      statusWriter.println("Current user bootstrap completed.");

    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      logger.log(Level.SEVERE, "Failed to boostrap user:\n  " + errMsg.getEntity());
      ServletUtil.setResponse(resp, e);
    }
  }

  private User createUser(OAuthCredential credential) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    com.restfb.types.User fbUser =
        FacebookSocialNetworkProvider.fetchObject(fbClient, "me", com.restfb.types.User.class);
    User user = User.create(credential);
    user.setFirstName(fbUser.getFirstName());
    user.setLastName(fbUser.getLastName());
    ContactInfo contactInfo = new ContactInfo();
    user.setContactInfo(contactInfo);
    contactInfo.setEmail(fbUser.getEmail());
    return user;
  }
}
