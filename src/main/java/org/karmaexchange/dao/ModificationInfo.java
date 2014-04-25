package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;

import lombok.Data;

@Data
public final class ModificationInfo {

  private KeyWrapper<User> creationUser;
  private Date creationDate;

  private KeyWrapper<User> lastModificationUser;
  private Date lastModificationDate;

  public static ModificationInfo create() {
    ModificationInfo info = new ModificationInfo();
    info.creationDate = new Date();
    info.lastModificationDate = info.creationDate;
    if (getCurrentUserKey() != null) {
      info.creationUser = KeyWrapper.create(getCurrentUserKey());
      info.lastModificationUser = info.creationUser;
    }
    return info;
  }

  public void update() {
    lastModificationUser = KeyWrapper.create(getCurrentUserKey());
    lastModificationDate = new Date();
  }
}
