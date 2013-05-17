package org.karmaexchange.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Some basic utilities for manipulating urls.
 *
 * @author Jeff Schnitzer
 * @author Amir Valiani
 */
public class URLUtil {

  public static String buildURL(URI base, Map<String, Object> params) {
    if (params.isEmpty()) {
      return base.toString();
    }
    return base + "?" + buildQueryString(params);
  }

  /**
   * Create a query string
   */
  public static String buildQueryString(Map<String, Object> params) {
    StringBuilder bld = new StringBuilder();

    boolean afterFirst = false;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      if (afterFirst)
        bld.append("&");
      else
        afterFirst = true;

      bld.append(urlEncode(entry.getKey()));
      bld.append("=");
      bld.append(urlEncode(entry.getValue()));
    }

    return bld.toString();
  }

  /**
   * An interface to URLEncoder.encode() that isn't inane
   */
  public static String urlEncode(Object value) {
    try {
      return URLEncoder.encode(value.toString(), "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
