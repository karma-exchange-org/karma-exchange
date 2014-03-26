package org.karmaexchange.dao;

import java.util.Date;
import java.util.List;

import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

import com.googlecode.objectify.annotation.Index;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public abstract class BaseEvent<T extends BaseEvent<T>> extends IdBaseDao<T> {

  public static final int MAX_EVENT_KARMA_POINTS = 500;

  protected String title;
  protected String description;
  protected Location location;

  @Index
  protected Date startTime;
  @Index
  protected Date endTime;

  /**
   * The number of karma points earned by participating in the event. This is derived from the
   * start and end time.
   */
  protected int karmaPoints;

  protected void initKarmaPoints() {
    long eventDurationMins = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
    karmaPoints = (int) Math.min(eventDurationMins, MAX_EVENT_KARMA_POINTS);
  }

  /*
   * Location is not validated.
   * Start-time can equal end-time for non karma generating events.
   */
  protected void validateBaseEvent(List<ValidationError> validationErrors) {
    if ((null == title) || title.isEmpty()) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "title"));
    }
    // TODO(avaliani): make sure the description has a minimum length. Avoiding this for now
    //   since we don't have auto event creation.
    if (null == startTime) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "startTime"));
    }
    if ( (startTime != null) && (endTime != null) && endTime.before(startTime)) {
      validationErrors.add(new MultiFieldResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_MUST_BE_GTEQ_SPECIFIED_FIELD,
        "endTime", "startTime"));
    }
  }
}
