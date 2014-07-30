package org.karmaexchange.resources.derived;

import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.GlobalUidType;
import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseSourceUser {

  private String firstName;
  private String lastName;
  private String email;

  public BaseSourceUser(User user) {
    firstName = user.getFirstName();
    lastName= user.getLastName();
    // If the primary email changes we need new mapping info at the source db.
    // TODO(avaliani): Think about how to address this. One way to store for each db
    //   the contact id.
    //   We need to handle correctly the case that a user changes their primary email
    //   address.
    email = user.getPrimaryEmail();
  }

  public Key<GlobalUidMapping> createGlobalUidMappingKey() {
    return GlobalUidMapping.getKey(
      new GlobalUid(GlobalUidType.EMAIL, email));
  }

  public UserInfo createUser(Key<Organization> eventListingOrgKey) {
    User newUser = User.create();
    newUser.setFirstName(firstName);
    newUser.setLastName(lastName);
    newUser.getRegisteredEmails().add(new RegisteredEmail(email, true));
    newUser.getEventOrgs().add(KeyWrapper.create(eventListingOrgKey));
    return new UserInfo(newUser);
  }

}
