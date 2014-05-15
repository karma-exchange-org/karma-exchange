package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidMapping;

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

  private GlobalUid globalUid;

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

  public static void trackAccess(GlobalUid globalUid, User user) {
    UserUsage usage = new UserUsage(
      GlobalUidMapping.getKey(globalUid),
      NAME_FIELD_VALUE,
      globalUid,
      user.getFirstName(),
      user.getLastName(),
      new Date(),
      user.getPrimaryEmail());
    ofy().save().entity(usage);
  }

}
