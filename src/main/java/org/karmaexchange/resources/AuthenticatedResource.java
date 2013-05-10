package org.karmaexchange.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import lombok.Getter;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

// TODO(avaliani): remove oAuth filter and switch every resource to an AuthenticatedResource
public class AuthenticatedResource {

  @Getter
  private OAuthCredential credential;
  @Getter
  private User user;

  public AuthenticatedResource(@Context HttpServletRequest servletRequest) {
    credential = SocialNetworkProviderFactory.getLoginProviderCredential(servletRequest);
    if (credential == null) {
      throw ErrorResponseMsg.createException("authentication credentials missing",
        ErrorInfo.Type.AUTHENTICATION);
    }
    user = User.getUser(credential);
    if (user == null) {
      // The user should be auto-created via the oAuth filter.
      throw ErrorResponseMsg.createException("user account does not exist",
        ErrorInfo.Type.AUTHENTICATION);
    }
  }
}
