package org.karmaexchange.security;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
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

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;

import com.google.common.collect.Lists;
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

    OAuthCredential credential = SocialNetworkProviderFactory.getLoginProviderCredential(req);
    if (credential == null) {
      setErrorResponse(resp, "login credentials not specified");
      return;
    }

    log.log(OAUTH_LOG_LEVEL, "[" + credential + "] checking if oAuth token is cached");

    // 1. If the oauth token is cached, then its valid. Just use it.
    if (!credentialIsCached(credential)) {

      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] verifying oauth token");

      // 2. If it's not cached, check with the social network provider if it's valid.
      if (verifyCredential(credential)) {

        log.log(OAUTH_LOG_LEVEL, "[" + credential + "] updating oauth token");

        // The token is valid, so we need to update the token.
        updateCachedCredential(credential);
      } else {
        log.log(OAUTH_LOG_LEVEL, "[" + credential + "] rejecting request");
        setErrorResponse(resp, "failed to validate oAuth token");
        return;
      }
    }

    // Authorization complete... continue to the resource.
    filters.doFilter(req, resp);
  }

  public static boolean credentialIsCached(OAuthCredential credential) {
    // The assumption here is that it is impossible to persist a fake provider + uid + token.
    // When we create the user object we will always construct the globalUidAndToken from scratch.
    // TODO(avaliani): Validate that Objectify @Cached is actually being hit.
    Iterable<Key<User>> keys = ofy().load().type(User.class)
        .filter("oauthCredentials.globalUidAndToken", credential.getGlobalUidAndToken())
        .keys();
    return !Lists.newArrayList(keys).isEmpty();
  }

  private static boolean verifyCredential(OAuthCredential credential) {
    SocialNetworkProvider provider =
        SocialNetworkProviderFactory.getProvider(credential.getProvider());
    if (provider == null) {
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] invalid provider in credential");
      return false;
    } else {
      return provider.verifyCredential(credential);
    }
  }

  private static void updateCachedCredential(OAuthCredential credential) {
    User user = ofy().load().type(User.class)
        .filter("oauthCredentials.globalUid", credential.getGlobalUid())
        .first()
        .now();
    if (user != null) {
      updateCachedCredential(user, credential);
    } else {
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] user has not yet been created");
    }
    // If the user does not exist the user has most likely not been created yet.
  }

  private static void updateCachedCredential(User user, OAuthCredential newCredential) {
    for (OAuthCredential credential : user.getOauthCredentials()) {
      if (credential.getGlobalUid().equals(newCredential.getGlobalUid())) {
        credential.setToken(newCredential.getToken());
        ofy().save().entity(user).now();
        return;
      }
    }
    log.log(OAUTH_LOG_LEVEL, "[" + newCredential + "] " +
    		"User found by credentials, but credential does not exist for updating");
  }

  private static void setErrorResponse(HttpServletResponse resp, String message) {
    // TODO(avaliani): copy facebook w/ a json / html response. Forbidden is not suppose to be used
    // for failed authentication.
    log.log(OAUTH_LOG_LEVEL, "Failed to authenticate: " + message);
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
  }
}
