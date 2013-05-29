package org.karmaexchange.dao;

import static java.lang.String.format;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Id;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public abstract class NameBaseDao<T extends NameBaseDao<T>> extends BaseDao<T> {
  @Id
  protected String name;

  @Override
  final protected void syncKeyAndIdForUpsert() {
    if (name == null) {
      throw ErrorResponseMsg.createException("resource key-name must be set",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (getKey() == null) {
      updateKey();
    } else if (!getKey().equals(Key.create(this).getString())) {
      throw ErrorResponseMsg.createException(
        format("resource key [%s] does not match name based key [%s]",
          getKey(), Key.create(this).getString()),
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  @Override
  public boolean isKeyComplete() {
    return name != null;
  }
}
