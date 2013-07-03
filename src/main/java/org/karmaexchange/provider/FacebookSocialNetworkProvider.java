package org.karmaexchange.provider;

import static java.lang.String.format;
import static org.karmaexchange.security.OAuthFilter.OAUTH_LOG_LEVEL;

import java.util.logging.Logger;

import javax.servlet.Filter;

import org.karmaexchange.dao.ContactInfo;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookOAuthException;

public final class FacebookSocialNetworkProvider extends SocialNetworkProvider {

  private static final Logger log = Logger.getLogger(Filter.class.getName());

  private static final String PROFILE_IMAGE_URL_FMT = "https://graph.facebook.com/%s/picture";

  public FacebookSocialNetworkProvider(OAuthCredential credential,
      SocialNetworkProviderType providerType) {
    super(credential, providerType);
  }

  @Override
  public boolean verifyCredential() {
    return verifyCredential(credential);
  }

  public static boolean verifyCredential(OAuthCredential credential) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    com.restfb.types.User fbUser =
        fetchObject(fbClient, "me", com.restfb.types.User.class, Parameter.with("fields", "id"));
    if (fbUser.getId().equals(credential.getUid())) {
      return true;
    } else {
      log.log(OAUTH_LOG_LEVEL,
        format("Uid of user=[%s] does not match credential uid=[%s]",
          fbUser.getId(), credential.getUid()));
      return false;
    }
  }

  @Override
  public User createUser() {
    return createUser(credential);
  }

  public static User createUser(OAuthCredential credential) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    com.restfb.types.User fbUser = fetchObject(fbClient, "me", com.restfb.types.User.class);
    User user = User.create(credential);
    user.setFirstName(fbUser.getFirstName());
    user.setLastName(fbUser.getLastName());
    ContactInfo contactInfo = new ContactInfo();
    user.setContactInfo(contactInfo);
    contactInfo.setEmail(fbUser.getEmail());
    return user;
  }

  @Override
  public String getProfileImageUrl() {
    return String.format(PROFILE_IMAGE_URL_FMT, credential.getUid());
  }

  public static <T> T fetchObject(FacebookClient fbClient, String name, Class<T> objClass,
      Parameter... parameters) {
    try {
      return fbClient.fetchObject(name, objClass, parameters);
    } catch(FacebookException e) {
      throw ErrorResponseMsg.createException(e,
        (e instanceof FacebookOAuthException) ? ErrorInfo.Type.AUTHENTICATION :
          ErrorInfo.Type.PARTNER_SERVICE_FAILURE);
    }
  }
}
