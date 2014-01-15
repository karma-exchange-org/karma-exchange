package org.karmaexchange.security;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
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
import org.karmaexchange.dao.UserUsage;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.ServletUtil;
import org.karmaexchange.util.UserService;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class OAuthFilter implements Filter {

  public static final Level OAUTH_LOG_LEVEL = Level.FINE;
  private static final Logger log = Logger.getLogger(OAuthFilter.class.getName());

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
    try {
      credential = getCredential(req);
    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      log.log(OAUTH_LOG_LEVEL, "Failed to get credentials:\n  " + errMsg.getEntity());
      ServletUtil.setResponse(resp, e);
      return;
    }

    // Authorization complete. Continue to the API entry point.
    if (credential != null) {
      UserService.setCurrentUser(credential, User.getKey(credential));
    }
    try {
      filters.doFilter(req, resp);
    } finally {
      if (credential != null) {
        UserService.clearCurrentUser();
      }
    }
  }

  private OAuthCredential getCredential(HttpServletRequest req) {
    OAuthCredential credential = SocialNetworkProviderFactory.getLoginProviderCredential(req);
    if (credential == null) {
      return null;
    }

    AsyncMemcacheService asyncCache = MemcacheServiceFactory.getAsyncMemcacheService();
    asyncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));

    log.log(OAUTH_LOG_LEVEL, "[" + credential + "] checking if oAuth token is cached");

    // 1. If the oauth token is cached, then it's valid. Just use it.
    if (!credentialIsCached(asyncCache, credential)) {

      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] verifying oauth token");

      // 2. If it's not cached, check with the social network provider if it's valid.
      if (verifyCredential(credential)) {

        log.log(OAUTH_LOG_LEVEL, "[" + credential + "] updating oauth token");

        // The token is valid, so we need to update the token.
        updateCachedCredential(asyncCache, credential);
      } else {
        throw ErrorResponseMsg.createException("authentication credentials are not valid",
          ErrorInfo.Type.AUTHENTICATION);
      }
    }

    return credential;
  }

  private static boolean credentialIsCached(AsyncMemcacheService asyncCache,
      OAuthCredential credential) {
    try {
      return asyncCache.get(credential.getGlobalUidAndToken()).get() != null;
    } catch (ExecutionException e) {
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] memcache get execution exception");
      return false;
    } catch (InterruptedException e) {
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] memcache get iterrupted exception");
      return false;
    }
  }

  private static boolean verifyCredential(OAuthCredential credential) {
    return SocialNetworkProviderFactory.getProvider(credential).verifyCredential();
  }

  private static void updateCachedCredential(AsyncMemcacheService asyncCache,
      OAuthCredential credential) {
    // TODO(avaliani): set cache expiration based on token
    asyncCache.put(credential.getGlobalUidAndToken(), Boolean.TRUE,
      Expiration.byDeltaSeconds(10*60));
    UserUsage.trackAccess(User.getKey(credential));
  }

}
