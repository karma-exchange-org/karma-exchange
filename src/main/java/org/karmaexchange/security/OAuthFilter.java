package org.karmaexchange.security;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.UserService;

import com.googlecode.objectify.Key;

public class OAuthFilter implements Filter {

  public static final Level OAUTH_LOG_LEVEL = Level.OFF;
  private static final Logger log = Logger.getLogger(Filter.class.getName());

  @SuppressWarnings("unused")
  private FilterConfig filterConfig;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  @Override
  public void destroy() {
    filterConfig = null;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filters)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    OAuthCredential credential;
    Key<User> userKey;
    try {
      credential = SocialNetworkProviderFactory.getLoginProviderCredential(req);
      if (credential == null) {
        throw ErrorResponseMsg.createException("authentication credentials missing",
          ErrorInfo.Type.AUTHENTICATION);
      }

      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] checking if oAuth token is cached");

      // 1. If the oauth token is cached, then its valid. Just use it.
      userKey = credentialIsCached(credential);
      if (userKey == null) {

        log.log(OAUTH_LOG_LEVEL, "[" + credential + "] verifying oauth token");

        // 2. If it's not cached, check with the social network provider if it's valid.
        if (verifyCredential(credential)) {

          log.log(OAUTH_LOG_LEVEL, "[" + credential + "] updating oauth token");

          // The token is valid, so we need to update the token.
          userKey = updateCachedCredential(credential);
        } else {
          throw ErrorResponseMsg.createException("authentication credentials are not valid",
            ErrorInfo.Type.AUTHENTICATION);
        }
      }
    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      log.log(OAUTH_LOG_LEVEL, "Failed to authenticate: " + errMsg);
      resp.setStatus(errMsg.getStatus());
      return;
    }

    // Authorization complete. Continue to the resource.
    try {
      UserService.updateCurrentUser(credential, userKey);
      filters.doFilter(req, resp);
    } finally {
      UserService.updateCurrentUser(null, null);
    }
  }

  public static Key<User> credentialIsCached(OAuthCredential credential) {
    // The assumption here is that it is impossible to persist a fake provider + uid + token.
    // When we create the user object we will always construct the globalUidAndToken from scratch.
    // TODO(avaliani): Validate that Objectify @Cached is actually being hit.
    Iterable<Key<User>> keys = ofy().load().type(User.class)
        .filter("oauthCredentials.globalUidAndToken", credential.getGlobalUidAndToken())
        .keys();
    Iterator<Key<User>> keysIter = keys.iterator();
    return keysIter.hasNext() ? keysIter.next() : null;
  }

  private static boolean verifyCredential(OAuthCredential credential) {
    SocialNetworkProvider provider =
        SocialNetworkProviderFactory.getProvider(credential);
    if (provider == null) {
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] invalid provider in credential");
      return false;
    } else {
      return provider.verifyCredential();
    }
  }

  private static Key<User> updateCachedCredential(OAuthCredential credential) {
    User user = User.getUser(credential);
    if (user == null) {
      user = createUser(credential);
    } else {
      updateCachedCredential(user, credential);
    }
    return Key.create(user);
  }

  /**
   * Populate the user based upon information stored in the oAuth provider.
   */
  private static User createUser(OAuthCredential credential) {
    User user = SocialNetworkProviderFactory.getProvider(credential).initUser();
    // getCurrentUserKey() is not setup at this point so we can't use BaseDao.upsert().
    ofy().save().entity(user).now();
    return user;
  }

  private static void updateCachedCredential(User user, OAuthCredential newCredential) {
    for (OAuthCredential credential : user.getOauthCredentials()) {
      if (credential.getGlobalUid().equals(newCredential.getGlobalUid())) {
        credential.setToken(newCredential.getToken());
        // getCurrentUserKey() is not setup at this point so we can't use BaseDao.upsert().
        // TODO(avaliani): consider moving this to a new non user object. This way we don't
        //     violate the atomicity of user object updates.
        ofy().save().entity(user).now();
        return;
      }
    }
    log.log(OAUTH_LOG_LEVEL, "[" + newCredential + "] " +
    		"User found by credentials, but credential does not exist for updating");
  }
}
