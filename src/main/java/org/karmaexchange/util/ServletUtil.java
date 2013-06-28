package org.karmaexchange.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class ServletUtil {

  private static final Logger logger = Logger.getLogger(ServletUtil.class.getName());

  public static void setResponse(HttpServletResponse resp, WebApplicationException err)
      throws IOException {
    Response errMsg = err.getResponse();
    resp.setStatus(errMsg.getStatus());
    resp.setContentType("application/json");
    OutputStream out = resp.getOutputStream();
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(out, errMsg.getEntity());
    } catch (JsonMappingException e) {
      logger.log(Level.WARNING, "Failed to error response to json: " + e.getMessage());
    }
    out.flush();
  }

}
