package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Waiver extends IdBaseDao<Waiver> {

  private String description;
  private String embeddedContent;

  public static void insert(Key<Organization> orgKey, @Nullable Waiver waiver) {
    waiver.initAndValidateOwner(orgKey);
    BaseDao.upsert(waiver);
  }

  private void initAndValidateOwner(Key<Organization> orgKey) {
    if (owner == null) {
      owner = orgKey;
    } else if (!owner.equals(orgKey)) {
      throw ValidationErrorInfo.createException(ImmutableList.of(
        new ResourceValidationError(this,
          ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "owner")));
    }
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    validateUpsert();
  }

  @Override
  protected void processUpdate(Waiver prevWaiver) {
    super.processUpdate(prevWaiver);
    validateUpsert();
  }

  private void validateUpsert() {
    List<ValidationError> validationErrors = Lists.newArrayList();

    if (description == null) {
      validationErrors.add(new ResourceValidationError(this,
        ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "description"));
    }

    if (embeddedContent == null) {
      validationErrors.add(new ResourceValidationError(this,
        ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "embeddedContent"));
    }

    if (!validationErrors.isEmpty()) {
      throw ValidationErrorInfo.createException(validationErrors);
    }
  }

  @Override
  protected Permission evalPermission() {
    BaseDao<?> ownerDao = (BaseDao<?>) ofy().load().key(owner).now();
    return ownerDao.getPermission();
  }
}
