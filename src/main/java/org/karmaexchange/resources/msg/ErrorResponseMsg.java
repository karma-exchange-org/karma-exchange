package org.karmaexchange.resources.msg;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Throwables;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public class ErrorResponseMsg {
  // We have this level of nesting to mimic facebook's nesting for error messages.
  // TODO(avaliani): investigate this more.
  private ErrorInfo error;

  public static WebApplicationException createException(Throwable e, ErrorInfo.Type errorType) {
    ErrorResponseMsg msg = new ErrorResponseMsg(new ErrorInfo(e, errorType));
    return new WebApplicationException(createErrorResponse(msg));
  }

  public static WebApplicationException createException(String message, ErrorInfo.Type errorType) {
    ErrorResponseMsg msg = new ErrorResponseMsg(new ErrorInfo(message, errorType));
    return new WebApplicationException(createErrorResponse(msg));
  }

  public static WebApplicationException createException(ErrorInfo errorInfo) {
    ErrorResponseMsg msg = new ErrorResponseMsg(errorInfo);
    return new WebApplicationException(createErrorResponse(msg));
  }

  private static Response createErrorResponse(ErrorResponseMsg msg) {
    return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
  }

  private ErrorResponseMsg(ErrorInfo error) {
    this.error = error;
  }

  @Data
  @NoArgsConstructor
  public static class ErrorInfo {
    private String message;
    private Type type;
    private String stackTrace;
    // private int code;
    // private int subcode

    public ErrorInfo(String message, Type type, @Nullable Throwable e) {
      this.message = message;
      this.type = type;
      if (e != null) {
        this.stackTrace = Throwables.getStackTraceAsString(e);
      }
    }

    public ErrorInfo(String message, Type type) {
      this(message, type, null);
    }

    public ErrorInfo(Throwable e, Type type) {
      this(e.getMessage(), type, e);
    }

    public enum Type {
      AUTHENTICATION,
      UNREGISTERED_USER,
      BACKEND_SERVICE_FAILURE,
      BAD_REQUEST,
      NOT_AUTHORIZED,
      VALIDATION_FAILURE,
      LIMIT_REACHED,
      PARTNER_SERVICE_FAILURE
    }
  }
}
