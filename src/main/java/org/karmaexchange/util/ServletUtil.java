package org.karmaexchange.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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

}
