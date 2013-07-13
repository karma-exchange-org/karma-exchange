package org.karmaexchange.resources.msg;

import java.util.List;

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
    RESOURCE_FIELD_VALUE_MUST_BE_GT_SPECIFIED_FIELD,
    RESOURCE_FIELD_VALUE_MUST_BE_GTEQ_LIMIT,
    RESOURCE_FIELD_VALUE_UNMODIFIABLE
  }

  private List<ValidationError> validationErrors;

  public static WebApplicationException createException(
      List<ValidationError> validationErrors) {
    return ErrorResponseMsg.createException(new ValidationErrorInfo(validationErrors));
  }

  private ValidationErrorInfo(List<ValidationError> validationErrors) {
    super("Validation failure", Type.VALIDATION_FAILURE);
    this.validationErrors = validationErrors;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationError {
    private ValidationErrorType errorType;
  }
}
