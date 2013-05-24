package org.karmaexchange.provider;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.common.collect.Maps;

public final class SocialNetworkProviderFactory {

  private static final String OAUTH_UID_SUFFIX = "uid";
  private static final String OAUTH_TOKEN_SUFFIX = "token";
  private static final String OAUTH_LOGIN = "login";

  public static SocialNetworkProvider getProvider(OAuthCredential credential) {
    return getProviderType(credential).getProvider(credential);
  }

  public static SocialNetworkProviderType getProviderType(OAuthCredential credential) {
    try {
      return SocialNetworkProviderType.valueOf(credential.getProvider().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw ErrorResponseMsg.createException(
        "unknown social network provider type: " + credential.getProvider(),
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  public static OAuthCredential getLoginProviderCredential(HttpServletRequest req) {
    Map<String, Cookie> cookies = getCookies(req);
    String provider = getCookieValue(cookies, OAUTH_LOGIN);
    if (provider != null) {
      String uid = getCookieValue(cookies, getProviderCookieName(provider, OAUTH_UID_SUFFIX));
      String token = getCookieValue(cookies, getProviderCookieName(provider, OAUTH_TOKEN_SUFFIX));
      if ((uid != null) && (token != null)) {
        return OAuthCredential.create(provider, uid, token);
      }
    }
    return null;
  }

  private static Map<String, Cookie> getCookies(HttpServletRequest req) {
    Map<String, Cookie> cookiesMap = Maps.newHashMap();
    Cookie[] cookiesArray = req.getCookies();
    if (cookiesArray != null) {
      for (Cookie cookie : cookiesArray) {
        cookiesMap.put(cookie.getName(), cookie);
      }
    }
    return cookiesMap;
  }

  private static String getCookieValue(Map<String, Cookie> cookiesMap, String cookieName) {
    Cookie cookie = cookiesMap.get(cookieName);
    return (cookie == null) ? null : cookie.getValue();
  }

  private static String getProviderCookieName(String provider, String cookieSuffix) {
    return provider + "-" + cookieSuffix;
  }

  // Never instantiated.
  private SocialNetworkProviderFactory() {
  }
}
