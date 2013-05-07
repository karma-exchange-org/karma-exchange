package org.karmaexchange.provider;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.karmaexchange.dao.OAuthCredential;

import com.google.common.collect.Maps;

public final class SocialNetworkProviderFactory {

  private static final String OAUTH_UID_SUFFIX = "uid";
  private static final String OAUTH_TOKEN_SUFFIX = "token";
  private static final String OAUTH_LOGIN = "login";

  private static final String FACEBOOK_PROVIDER = "facebook";

  private static final Map<String, SocialNetworkProvider> PROVIDER_MAP = Maps.newHashMap();
  static {
    PROVIDER_MAP.put(FACEBOOK_PROVIDER, new FacebookSocialNetworkProvider());
  }

  public static SocialNetworkProvider getProvider(String provider) {
    return PROVIDER_MAP.get(provider);
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
