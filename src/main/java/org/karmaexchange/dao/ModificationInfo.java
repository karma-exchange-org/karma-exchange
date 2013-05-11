package org.karmaexchange.dao;

import java.util.Date;

import javax.annotation.Nullable;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;

import lombok.Data;

@Data
@Embed
public final class ModificationInfo {

  private KeyWrapper<User> creationUser;
  private Date creationDate;

  private KeyWrapper<User> lastModificationUser;
  private Date lastModificationDate;

  public static ModificationInfo create() {
    return create(null);
  }

  public static ModificationInfo create(@Nullable Key<User> currentUserKey) {
    ModificationInfo info = new ModificationInfo();
    info.creationDate = new Date();
    info.lastModificationDate = info.creationDate;
    if (currentUserKey != null) {
      info.creationUser = KeyWrapper.create(currentUserKey);
      info.lastModificationUser = info.creationUser;
    }
    return info;
  }

  public void update(Key<User> currentUserKey) {
    lastModificationUser = KeyWrapper.create(currentUserKey);
    lastModificationDate = new Date();
  }
}
