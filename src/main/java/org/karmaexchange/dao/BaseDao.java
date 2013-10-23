package org.karmaexchange.dao;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlTransient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.karmaexchange.resources.msg.AuthorizationErrorInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.UserService;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.Parent;

@Data
public abstract class BaseDao<T extends BaseDao<T>> {

  @Parent
  protected Key<?> owner;
  @Ignore
  private String key;
  private ModificationInfo modificationInfo;

  @Ignore
  protected Permission permission;

  public final void setOwner(String keyStr) {
    owner = (keyStr == null) ? null : Key.<Object>create(keyStr);
  }

  public final String getOwner() {
    return (owner == null) ? null : owner.getString();
  }

  public static <T extends BaseDao<T>> void upsert(T resource) {
    ofy().transact(new UpsertTxn<T>(resource));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class UpsertTxn<T extends BaseDao<T>> extends VoidWork {
    private final T resource;

    public void vrun() {
      // Cleanup any id and key mismatch.
      resource.syncKeyAndIdForUpsert();

      T prevResource = null;
      if (resource.isKeyComplete()) {
        prevResource = ofy().load().key(Key.create(resource)).now();
      }
      if (prevResource == null) {
        resource.insert();
      } else {
        resource.update(prevResource);
      }
    }
  }

  protected abstract void syncKeyAndIdForUpsert();

  @XmlTransient
  public abstract boolean isKeyComplete();

  public static <T extends BaseDao<T>> void partialUpdate(T resource) {
    resource.partialUpdate();
  }

  public static <T extends BaseDao<T>> void delete(Key<T> key) {
    ofy().transact(new DeleteTxn<T>(key));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class DeleteTxn<T extends BaseDao<T>> extends VoidWork {
    private final Key<T> resourceKey;

    public void vrun() {
      T resource = ofy().load().key(resourceKey).now();
      if (resource != null) {
        resource.delete();
      }
    }
  }

  final void insert() {
    preProcessInsert();
    validateMutationPermission();
    ofy().save().entity(this).now();
    postProcessInsert();
  }

  final void update(T prevObj) {
    processUpdate(prevObj);
    validateMutationPermission();
    ofy().save().entity(this).now();
  }

  final void partialUpdate() {
    processPartialUpdate(null);
    // Partial updates have task-specific permission rules vs. per object type permission rules.
    // But all mutations require either a logged in user or an admin user.
    validateLoginStatusForMutation();
    ofy().save().entity(this).now();
  }

  final void delete() {
    processDelete();
    validateMutationPermission();
    ofy().delete().key(Key.create(this)).now();
  }

  private void validateMutationPermission() {
    validateLoginStatusForMutation();
    updatePermission();
    if (!permission.canEdit()) {
      throw AuthorizationErrorInfo.createException(this);
    }
  }

  private void validateLoginStatusForMutation() {
    if (UserService.isNotLoggedInUser()) {
      throw ErrorResponseMsg.createException("Login required", ErrorInfo.Type.LOGIN_REQUIRED);
    }
  }

  protected void preProcessInsert() {
    setModificationInfo(ModificationInfo.create());
  }

  protected void postProcessInsert() {
    updateKey();
  }

  protected void processUpdate(T prevObj) {
    updateKey();
    if (!prevObj.getKey().equals(getKey())) {
      throw ErrorResponseMsg.createException(
        format("thew new resource key [%s] does not match the previous key [%s]",
          getKey(), prevObj.getKey()),
        ErrorInfo.Type.BAD_REQUEST);
    }
    processPartialUpdate(prevObj);
  }

  final void processPartialUpdate(T prevObj) {
    if (getModificationInfo() == null) {
      // Handle objects that were created without modification info.
      if ((prevObj == null) || (prevObj.getModificationInfo() == null)) {
        setModificationInfo(ModificationInfo.create());
      } else {
        setModificationInfo(prevObj.getModificationInfo());
      }
    }
    getModificationInfo().update();
  }

  protected void processDelete() {
  }

  @OnLoad
  public void processLoad() {
    updateKey();
    updatePermission();
  }

  protected void updateKey() {
    setKey(KeyWrapper.create(this).getKey());
  }

  protected final void updatePermission() {
    if (UserService.isCurrentUserAdmin()) {
      permission = Permission.ALL;
    } else if (UserService.isNotLoggedInUser()) {
      permission = Permission.READ;
    } else {
      permission = evalPermission();
    }
  }

  protected abstract Permission evalPermission();

  public void updateDependentNamedKeys() {
    // Do nothing by default.
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class ResourceValidationError extends ValidationError {

    @Nullable
    private String resourceKind;
    @Nullable
    private String resourceKey;
    @Nullable
    private String field;

    public ResourceValidationError(BaseDao<?> resource, ValidationErrorType errorType,
        @Nullable String fieldName) {
      super(errorType);
      if (resource.isKeyComplete()) {
        resourceKey = Key.create(resource).getString();
      }
      resourceKind = OfyUtil.getKind(resource.getClass());
      this.field = fieldName;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class MultiFieldResourceValidationError extends ResourceValidationError {

    private String otherField;

    public MultiFieldResourceValidationError(BaseDao<?> resource, ValidationErrorType errorType,
        String fieldName, String otherFieldName) {
      super(resource, errorType, fieldName);
      this.otherField = otherFieldName;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class LimitResourceValidationError extends ResourceValidationError {

    private int limit;

    public LimitResourceValidationError(BaseDao<?> resource, ValidationErrorType errorType,
        String fieldName, int limit) {
      super(resource, errorType, fieldName);
      this.limit = limit;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class ListValueValidationError extends ResourceValidationError {

    private String memberValue;

    public ListValueValidationError(BaseDao<?> resource, ValidationErrorType errorType,
        String fieldName, String memberValue) {
      super(resource, errorType, fieldName);
      this.memberValue = memberValue;
    }
  }
}
