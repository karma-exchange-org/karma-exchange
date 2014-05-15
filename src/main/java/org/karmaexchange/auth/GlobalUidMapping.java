package org.karmaexchange.auth;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.dao.User;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Data
@NoArgsConstructor
public class GlobalUidMapping {
  @Id
  private String globalUid;

  // This element needs to be indexed to delete mappings when a user is deleted.
  // TODO(avaliani): implement deletion of mappings when a user is deleted.
  @Index
  private Key<User> userKey;

  public GlobalUidMapping(GlobalUid globalUid, Key<User> userKey) {
    this.globalUid = globalUid.getId();
    this.userKey = userKey;
  }

  public static GlobalUidMapping load(GlobalUid globalUid) {
    return ofy().load().key(getKey(globalUid)).now();
  }

  public static Key<GlobalUidMapping> getKey(GlobalUid globalUid) {
    return Key.create(GlobalUidMapping.class, globalUid.getId());
  }
}
