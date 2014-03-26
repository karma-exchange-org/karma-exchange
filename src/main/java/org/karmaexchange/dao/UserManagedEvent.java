package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.resources.msg.BaseDaoView;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class UserManagedEvent
    extends BaseEvent<UserManagedEvent>
    implements BaseDaoView<UserManagedEvent> {

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    validateEvent();
    initKarmaPoints();
  }

  @Override
  protected void processUpdate(UserManagedEvent prevObj) {
    super.processUpdate(prevObj);
    validateEvent();
    initKarmaPoints();
  }

  private void validateEvent() {
    List<ValidationError> validationErrors = Lists.newArrayList();

    validateBaseEvent(validationErrors);
    if (owner == null) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "owner"));
    }

    if (!validationErrors.isEmpty()) {
      throw ValidationErrorInfo.createException(validationErrors);
    }
  }

  @Override
  protected Permission evalPermission() {
    if (owner.equals(getCurrentUserKey())) {
      return Permission.ALL;
    } else {
      return Permission.READ;
    }
  }

  @Override
  public UserManagedEvent getDao() {
    return this;
  }
}
