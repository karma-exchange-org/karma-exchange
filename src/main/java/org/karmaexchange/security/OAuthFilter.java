package org.karmaexchange.security;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.io.OutputStream;
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

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminTaskType;
import org.karmaexchange.util.UserService;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

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
    UpdateCredentialResult updateCredentialResult = null;
    AdminUtil.setCurrentUser(AdminTaskType.OAUTH_FILTER);
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
          updateCredentialResult = updateCachedCredential(credential);
          userKey = Key.create(updateCredentialResult.getUser());
        } else {
          throw ErrorResponseMsg.createException("authentication credentials are not valid",
            ErrorInfo.Type.AUTHENTICATION);
        }
      }
    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      log.log(OAUTH_LOG_LEVEL, "Failed to authenticate:\n  " + errMsg.getEntity());
      setResponse(resp, e);
      return;
    } finally {
      UserService.clearCurrentUser();
    }

    // Authorization complete. Continue to the resource.
    UserService.setCurrentUser(credential, userKey);
    try {
      if (updateCredentialResult != null) {
        updateCredentialResult.finalizeUpdate();
      }
      filters.doFilter(req, resp);
    } finally {
      UserService.clearCurrentUser();
    }
  }

  private static void setResponse(HttpServletResponse resp, WebApplicationException err)
      throws IOException {
    Response errMsg = err.getResponse();
    resp.setStatus(errMsg.getStatus());
    resp.setContentType("application/json");
    OutputStream out = resp.getOutputStream();
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(out, errMsg.getEntity());
    } catch (JsonMappingException e) {
      log.log(OAUTH_LOG_LEVEL, "Failed to write json error response: " + e.getMessage());
    }
    out.flush();
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
    return SocialNetworkProviderFactory.getProvider(credential).verifyCredential();
  }

  private static UpdateCredentialResult updateCachedCredential(OAuthCredential credential) {
    UpdateCredentialTxn updateCredentialTxn = new UpdateCredentialTxn(credential);
    ofy().transact(updateCredentialTxn);
    return updateCredentialTxn.getResult();
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class UpdateCredentialTxn extends VoidWork {
    private final OAuthCredential credential;
    private final SocialNetworkProvider socialNetworkProvider;
    private UpdateCredentialResult result;

    public UpdateCredentialTxn(OAuthCredential credential) {
      this.credential = credential;
      socialNetworkProvider = SocialNetworkProviderFactory.getProvider(credential);
    }

    public void vrun() {
      User user = BaseDao.load(User.createKey(credential));
      if (user == null) {
        result = new NewUserUpdateCredentialResult(createUser(), socialNetworkProvider);
      } else {
        updateCachedCredential(user);
        result = new UpdateCredentialResult(user);
      }
    }

    /**
     * Populate the user based upon information stored in the oAuth provider.
     */
    private User createUser() {
      User user = socialNetworkProvider.createUser();
      BaseDao.upsert(user);
      return user;
    }

    private void updateCachedCredential(User user) {
      for (OAuthCredential persistedCredential : user.getOauthCredentials()) {
        if (persistedCredential.getGlobalUid().equals(credential.getGlobalUid())) {
          persistedCredential.setToken(credential.getToken());
          BaseDao.partialUpdate(user);
          return;
        }
      }
      log.log(OAUTH_LOG_LEVEL, "[" + credential + "] " +
          "User found by credentials, but credential does not exist for updating");
    }
  }

  @Data
  private static class UpdateCredentialResult {
    protected final User user;

    /**
     * Completes the credential update tasks after {@link UserService#updateCurrentUser} is
     * invoked.
     */
    public void finalizeUpdate() {
      // No-op
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  private static class NewUserUpdateCredentialResult extends UpdateCredentialResult {
    private final SocialNetworkProvider socialNetworkProvider;

    public NewUserUpdateCredentialResult(User user, SocialNetworkProvider socialNetworkProvider) {
      super(user);
      this.socialNetworkProvider = socialNetworkProvider;
    }

    @Override
    public void finalizeUpdate() {
      User.updateProfileImage(Key.create(user), socialNetworkProvider.getProviderType());
    }
  }

}
