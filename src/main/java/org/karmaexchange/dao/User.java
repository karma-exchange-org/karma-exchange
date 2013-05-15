package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Cache
@Data
@EqualsAndHashCode(callSuper=false)
public final class User extends BaseDao<User> {

  @Id
  private Long id;
  @Ignore
  private String key;
  private ModificationInfo modificationInfo;

  @Ignore
  private Permission permission;

  @Index
  private String firstName;
  @Index
  private String lastName;
  @Index
  private String nickName;
  private Image profileImage;
  private ContactInfo contactInfo;
  private List<EmergencyContact> emergencyContacts = Lists.newArrayList();

  @Index
  private List<KeyWrapper<Cause>> causes = Lists.newArrayList();

  @Index
  private List<KeyWrapper<Skill>> skills = Lists.newArrayList();

  // Skipping interests for now.
  // Facebook has a detailed + categorized breakdown of interests.

  @Index
  private long karmaPoints;

  private Rating eventOrganizerRating;

  private EventSearch lastEventSearch;

  // TODO(avaliani): jackson doesn't like oAuth. It converts it to "oauth".
  private List<OAuthCredential> oauthCredentials = Lists.newArrayList();

  // TODO(avaliani): profileSecurityPrefs

  @Override
  public void setId(Long id) {
    this.id = id;
    updateKey();
  }

  @Override
  protected void processUpdate(User oldUser) {
    super.processUpdate(oldUser);
    // Some fields can not be manipulated by updating the user.
    // TODO(avlaiani): re-evaluate this. All fields should be updateable if you have admin
    //     privileges.
    // setKarmaPoints(oldUser.getKarmaPoints());
    // setEventOrganizerRating(oldUser.getEventOrganizerRating());
    setOauthCredentials(oldUser.getOauthCredentials());
  }

  @Override
  protected void updatePermission() {
    if (Key.create(this).equals(getCurrentUserKey())) {
      setPermission(Permission.ALL);
    } else {
      setPermission(Permission.READ);
    }
  }

  public static User getUser(OAuthCredential credential) {
    return loadFirst(ofy().load().type(User.class)
      .filter("oauthCredentials.globalUid", credential.getGlobalUid()));
  }
}
