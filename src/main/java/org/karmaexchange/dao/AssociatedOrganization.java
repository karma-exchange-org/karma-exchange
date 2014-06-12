package org.karmaexchange.dao;

import javax.annotation.Nullable;

import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class AssociatedOrganization extends NullableKeyWrapper<Organization> {

  public enum Association {
    EVENT_SPONSOR,
    EVENT_OWNER_ANCESTOR,
    EVENT_OWNER
  }

  private String orgName;
  private Association association;

  public AssociatedOrganization(@Nullable Key<Organization> orgKey, String orgName,
      Association association) {
    super(orgKey);
    this.orgName = orgName;
    this.association = association;
  }

  public AssociatedOrganization(Organization org, Association association) {
    super(Key.create(org));
    this.orgName = org.getOrgName();
    this.association = association;
  }

  public ValidationError validate(BaseDao<?> resource, String listFieldName) {
    if (orgName == null) {
      return new BaseDao.ListValueValidationError(
        resource, ValidationErrorType.RESOURCE_FIELD_LIST_VALUE_INVALID_VALUE, listFieldName,
        this.toString());
    }
    return null;
  }
}
