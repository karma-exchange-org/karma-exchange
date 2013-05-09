package org.karmaexchange.resources.msg;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
public class ResponseMsg {
  // We have this level of nesting to mimic facebook's nesting for error messages.
  // TODO(avaliani): investigate this more.
  private ErrorMsg error;

  public static WebApplicationException createException(Throwable e, ErrorMsg.Type errorType) {
    return new WebApplicationException(createErrorResponse(e.getMessage(), errorType));
  }

  public static WebApplicationException createException(String message, ErrorMsg.Type errorType) {
    return new WebApplicationException(createErrorResponse(message, errorType));
  }

  private static Response createErrorResponse(String message, ErrorMsg.Type errorType) {
    ResponseMsg msg = createErrorMsg(message, errorType);
    return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
  }

  private static ResponseMsg createErrorMsg(String message, ErrorMsg.Type errorType) {
    ErrorMsg errorMsg = new ErrorMsg(message, errorType);
    ResponseMsg msg = new ResponseMsg();
    msg.setError(errorMsg);
    return msg;
  }

  @Data
  @NoArgsConstructor
  public static class ErrorMsg {
    private String message;
    private Type type;
    // private int code;
    // private int subcode

    public ErrorMsg(String message, Type type) {
      this.message = message;
      this.type = type;
    }

    public enum Type {
      BAD_REQUEST,
      AUTHENTICATION,
      BACKEND_SERVICE_FAILURE,
      PARTNER_SERVICE_FAILURE
    }
  }
}
