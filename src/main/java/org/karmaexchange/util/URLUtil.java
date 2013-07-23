package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import com.google.common.collect.Multimap;

/**
 * Some basic utilities for manipulating urls.
 *
 * @author Jeff Schnitzer
 * @author Amir Valiani
 */
public class URLUtil {

  public static String buildURL(URI base, Multimap<String, String> params) {
    if (params.isEmpty()) {
      return base.toString();
    } else {
      return base + "?" + buildQueryString(params);
    }
  }

  /**
   * Create a query string
   */
  private static String buildQueryString(Multimap<String, String> params) {
    StringBuilder bld = new StringBuilder();

    boolean afterFirst = false;
    for (Map.Entry<String, String> entry : params.entries()) {
      if (afterFirst)
        bld.append("&");
      else
        afterFirst = true;

      bld.append(urlEncode(entry.getKey()));
      bld.append("=");
      checkNotNull(entry.getValue(), format("query parameter[%s] has no value", entry.getKey()));
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
