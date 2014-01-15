package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.User.RegisteredEmail;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

// Used to track user usage information. We need this because for the demo we periodically
// blow away the user objects so we don't have to deal with backwards compatability. Once
// we are live, this class will no longer be needed.
@XmlRootElement
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUsage {
  private static final String NAME_FIELD_VALUE = "1";

  @Parent
  private Key<?> owner;

  // The owner is unique. There is only one UserUsage entity per owner. Therefore, use
  // the same 'name' for all UserUsage entities.
  @Id
  private String name = NAME_FIELD_VALUE;

  private String firstName;
  private String lastName;
  private Date lastVisited;
  @Nullable
  private String email;

  public final void setOwner(String keyStr) {
    owner = (keyStr == null) ? null : Key.<Object>create(keyStr);
  }

  public final String getOwner() {
    return (owner == null) ? null : owner.getString();
  }

  public static void trackUser(User user) {
    UserUsage usage = new UserUsage(
      Key.create(user),
      NAME_FIELD_VALUE,
      user.getFirstName(),
      user.getLastName(),
      new Date(),
      getPrimaryEmail(user));
    ofy().save().entity(usage).now();
  }

  private static String getPrimaryEmail(User user) {
    for (RegisteredEmail registeredEmail : user.getRegisteredEmails()) {
      if (registeredEmail.isPrimary()) {
        return registeredEmail.getEmail();
      }
    }
    return null;
  }

  public static void trackAccess(Key<User> user) {
    UserAccess access = new UserAccess(
      user,
      NAME_FIELD_VALUE,
      new Date());
    ofy().save().entity(access).now();
  }

  // Used only to delete test user's history
  public static void deleteHistory(Key<User> userKey) {
    ofy().delete().key(getKeyForUser(userKey));
    ofy().delete().key(UserAccess.getKeyForUser(userKey));
  }

  private static Key<UserUsage> getKeyForUser(Key<User> userKey) {
    return Key.create(userKey, UserUsage.class, NAME_FIELD_VALUE);
  }

  @Entity
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class UserAccess {
    @Parent
    private Key<?> owner;

    // The owner is unique. There is only one UserAccess entity per owner. Therefore, use
    // the same 'name' for all UserAccess entities.
    @Id
    private String name = NAME_FIELD_VALUE;

    private Date lastVisited;

    private static Key<UserAccess> getKeyForUser(Key<User> userKey) {
      return Key.create(userKey, UserAccess.class, NAME_FIELD_VALUE);
    }
  }
}
