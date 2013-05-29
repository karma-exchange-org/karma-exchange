package org.karmaexchange.dao;

import static java.lang.String.format;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Id;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public abstract class IdBaseDao<T extends IdBaseDao<T>> extends BaseDao<T> {
  @Id
  private Long id;

  @Override
  final protected void syncKeyAndIdForUpsert() {
    if ((id == null) && (getKey() != null)) {
      throw ErrorResponseMsg.createException(
        format("resource key is set [%s] but resource id is not set", getKey()),
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (id != null) {
      if (getKey() == null) {
        updateKey();
      } else if (!getKey().equals(Key.create(this).getString())) {
        throw ErrorResponseMsg.createException(
          format("resource key [%s] does not match id based key [%s]",
            getKey(), Key.create(this).getString()),
          ErrorInfo.Type.BAD_REQUEST);
      }
    }
  }

  @Override
  public boolean isKeyComplete() {
    return id != null;
  }
}
