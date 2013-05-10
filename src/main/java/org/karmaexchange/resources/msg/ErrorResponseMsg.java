package org.karmaexchange.resources.msg;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
public class ErrorResponseMsg {
  // We have this level of nesting to mimic facebook's nesting for error messages.
  // TODO(avaliani): investigate this more.
  private ErrorInfo error;

  public static WebApplicationException createException(Throwable e, ErrorInfo.Type errorType) {
    return new WebApplicationException(createErrorResponse(e.getMessage(), errorType));
  }

  public static WebApplicationException createException(String message, ErrorInfo.Type errorType) {
    return new WebApplicationException(createErrorResponse(message, errorType));
  }

  private static Response createErrorResponse(String message, ErrorInfo.Type errorType) {
    ErrorResponseMsg msg = createErrorMsg(message, errorType);
    return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
  }

  private static ErrorResponseMsg createErrorMsg(String message, ErrorInfo.Type errorType) {
    ErrorInfo errorMsg = new ErrorInfo(message, errorType);
    ErrorResponseMsg msg = new ErrorResponseMsg();
    msg.setError(errorMsg);
    return msg;
  }

  @Data
  @NoArgsConstructor
  public static class ErrorInfo {
    private String message;
    private Type type;
    // private int code;
    // private int subcode

    public ErrorInfo(String message, Type type) {
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
