package org.karmaexchange.auth;

import org.karmaexchange.dao.User;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Data
@NoArgsConstructor
public class GlobalUidMapping {
  @Id
  private String globalUid;

  private Key<User> userKey;

  public GlobalUidMapping(GlobalUid globalUid, Key<User> userKey) {
    this.globalUid = globalUid.getId();
    this.userKey = userKey;
  }

  public static Key<GlobalUidMapping> getKey(GlobalUid globalUid) {
    return Key.create(GlobalUidMapping.class, globalUid.getId());
  }
}
