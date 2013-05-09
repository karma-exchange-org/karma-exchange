package org.karmaexchange.dao;

import java.util.Date;

import javax.annotation.Nullable;

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

  public static ModificationInfo create(@Nullable User currentUser) {
    ModificationInfo info = new ModificationInfo();
    info.creationDate = new Date();
    info.lastModificationDate = info.creationDate;
    if (currentUser != null) {
      info.creationUser = KeyWrapper.create(currentUser);
      info.lastModificationUser = info.creationUser;
    }
    return info;
  }

  public void update(User currentUser) {
    lastModificationUser = KeyWrapper.create(currentUser);
    lastModificationDate = new Date();
  }
}
