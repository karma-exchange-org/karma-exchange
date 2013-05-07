package org.karmaexchange.provider;

import static java.lang.String.format;
import static org.karmaexchange.security.OAuthFilter.OAUTH_LOG_LEVEL;

import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.Filter;

import org.karmaexchange.dao.OAuthCredential;

import com.restfb.DefaultFacebookClient;
import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;
import com.restfb.util.ReflectionUtils;

public final class FacebookSocialNetworkProvider extends SocialNetworkProvider {

  private static final Logger log = Logger.getLogger(Filter.class.getName());

  @Override
  public boolean verifyCredential(OAuthCredential credential) {
    DebugTokenFacebookClient fbClient = new DebugTokenFacebookClient(credential);
    try {
      DebugTokenInfo tokenInfo = fbClient.debugToken(credential.getToken());
      log.log(OAUTH_LOG_LEVEL, "DebugTokenInfo fetched: " + tokenInfo);
      if (tokenInfo.getUserId().equals(credential.getUid())) {
        return true;
      } else {
        log.log(OAUTH_LOG_LEVEL,
          format("Uid of tokenInfo=[%s] does not match credential uid=[%s], tokenInfo[%s]",
          tokenInfo.getUserId(), credential.getUid(), tokenInfo));
        return false;
      }
    } catch(FacebookException e) {
      log.log(OAUTH_LOG_LEVEL, "Failed to fetch credential", e);
      return false;
    }
  }

  // Temporary until https://github.com/revetkn/restfb/pull/53 makes it into a restfb maven jar.
  private static class DebugTokenFacebookClient extends DefaultFacebookClient {

    public DebugTokenFacebookClient(OAuthCredential credential) {
      super(credential.getToken());
    }

    public DebugTokenInfo debugToken(String inputToken) {
      verifyParameterPresence("inputToken", inputToken);

      String response = makeRequest("/debug_token", Parameter.with("input_token", inputToken));
      JsonObject json = new JsonObject(response);
      JsonObject data = json.getJsonObject("data");
      return getJsonMapper().toJavaObject(data.toString(), DebugTokenInfo.class);
    }
  }

  /**
   * <p>Represents the result of a {@link FacebookClient#debugToken(String)} inquiry.</p>
   *
   * FIXME does this class belong here?
   *
   * <p>See <a href="https://developers.facebook.com/docs/howtos/login/debugging-access-tokens/">
   * @author Broc Seib
   */
  public static class DebugTokenInfo {
    @Facebook("app_id")
    private String appId;

    @Facebook("application")
    private String application;

    @Facebook("expires_at")
    private Long expiresAt;

    @Facebook("issued_at")
    private Long issuedAt;

    @Facebook("is_valid")
    private Boolean isValid;

    @Facebook("user_id")
    private String userId;

    // FIXME let's read 'scopes' and 'metadata' if they exist. They are a nested structure...

    /**
     * The application id.
     *
     * @return The id of the application.
     */
    public String getAppId() {
      return appId;
    }

    /**
     * The application name.
     *
     * @return The name of the application.
     */
    public String getApplication() {
      return application;
    }

    /**
     * The date on which the access token expires.
     *
     * @return The date on which the access token expires.
     */
    public Date getExpiresAt() {
      // note that the expire timestamp is in *seconds*, not milliseconds
      return expiresAt == null ? null : new Date(expiresAt*1000L);
    }

    /**
     * The date on which the access token was issued.
     *
     * @return The date on which the access token was issued.
     */
    public Date getIssuedAt() {
      // note that the issue timestamp is in *seconds*, not milliseconds
      return issuedAt == null ? null : new Date(issuedAt*1000L);
    }

    /**
     * Whether or not the token is valid.
     *
     * @return Whether or not the token is valid.
     */
    public Boolean isValid() {
      return isValid;
    }

    /**
     * The user id.
     *
     * @return The user id.
     */
    public String getUserId() {
      return userId;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return ReflectionUtils.hashCode(this);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object that) {
      return ReflectionUtils.equals(this, that);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return ReflectionUtils.toString(this);
    }
  }
}