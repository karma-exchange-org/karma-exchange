package org.karmaexchange.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class ServletUtil {

  private static final Logger logger = Logger.getLogger(ServletUtil.class.getName());

  public static void setResponse(HttpServletResponse resp, WebApplicationException err)
      throws IOException {
    Response errMsg = err.getResponse();
    resp.setStatus(errMsg.getStatus());
    resp.setContentType("application/json");
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), "UTF-8"));
    writer.print(getExceptionResponseAsJsonString(err));
    writer.flush();
  }

  public static String getExceptionResponseAsJsonString(WebApplicationException errToDump) {
    Response errMsg = errToDump.getResponse();
    StringWriter sw = new StringWriter();
    Throwable errWhileDumping = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(sw, errMsg.getEntity());
    } catch (JsonGenerationException e) {
      errWhileDumping = e;
    } catch (JsonMappingException e) {
      errWhileDumping = e;
    } catch (IOException e) {
      errWhileDumping = e;
    }
    if (errWhileDumping != null) {
      logger.log(Level.WARNING,
        "Failed to error response to json: " + errWhileDumping.getMessage());
      return "Failed to convert response to json: " + errWhileDumping.getMessage();
    } else {
      return sw.toString();
    }
  }

  public static URI getRequestUri(HttpServletRequest req) {
    try {
      return new URI(req.getRequestURL().toString());
    } catch (URISyntaxException e) {
      // Impossible.
      throw new RuntimeException(e);
    }
  }

  public static String getBaseUri(HttpServletRequest req) {
    return getBaseUri(req, false);
  }

  public static String getBaseUriWithPort(HttpServletRequest req) {
    return getBaseUri(req, true);
  }

  private static String getBaseUri(HttpServletRequest req, boolean alwaysSpecifyPort) {
    URI requestUri = getRequestUri(req);
    int port = requestUri.getPort();
    String scheme = requestUri.getScheme();
    if ((port == -1) && alwaysSpecifyPort) {
      if (scheme.equalsIgnoreCase(new String(HttpURL.DEFAULT_SCHEME))) {
        port = HttpURL.DEFAULT_PORT;
      } else if (scheme.equalsIgnoreCase(new String(HttpsURL.DEFAULT_SCHEME))) {
        port = HttpsURL.DEFAULT_PORT;
      }
    }
    String portString = port == -1 ? "" : ":" + port;
    return scheme + "://" + requestUri.getHost() + portString;
  }

  public static Cookie getCookie(HttpServletRequest req, String cookieName) {
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equalsIgnoreCase(cookieName)) {
          return cookie;
        }
      }
    }
    return null;
  }
}
