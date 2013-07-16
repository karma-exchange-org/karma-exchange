package org.karmaexchange.resources.msg;

import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ValidationErrorInfo extends ErrorResponseMsg.ErrorInfo {

  public enum ValidationErrorType {
    RESOURCE_FIELD_VALUE_REQUIRED,
    RESOURCE_FIELD_VALUE_INVALID,
    RESOURCE_FIELD_VALUE_MUST_BE_GT_SPECIFIED_FIELD,
    RESOURCE_FIELD_VALUE_MUST_BE_GTEQ_LIMIT,
    RESOURCE_FIELD_VALUE_UNMODIFIABLE
  }

  private List<? extends ValidationError> validationErrors;

  public static WebApplicationException createException(
      List<? extends ValidationError> validationErrors) {
    return createException(validationErrors, null);
  }

  public static WebApplicationException createException(
      List<? extends ValidationError> validationErrors, @Nullable Throwable e) {
    return ErrorResponseMsg.createException(new ValidationErrorInfo(validationErrors, e));
  }

  private ValidationErrorInfo(List<? extends ValidationError> validationErrors,
      @Nullable Throwable e) {
    super("Validation failure", Type.VALIDATION_FAILURE, e);
    this.validationErrors = validationErrors;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationError {
    private ValidationErrorType errorType;
  }
}
