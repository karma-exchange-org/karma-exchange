package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.UserService;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
// TODO(avaliani): re-eval this caching strategy once OAuth caching is re-worked.
@Cache
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class User extends IdBaseDao<User> {

  /*
   * Note that User object creation via the oAuth filter does not leverage BaseDao to init
   * the user object.
   * TODO(avaliani): re-eval this. We can either factor out steps like preProcessInsert(). Or
   *     fix all of BaseDao to handle inserts without a current user key. The later seems like
   *     a cleaner approach.
   */

  @Index
  private String firstName;
  @Index
  private String lastName;
  @Index
  private String nickName;
  private ImageRef profileImage;
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

  // Initialization required for OAuth user creation.
  private IndexedAggregateRating eventOrganizerRating = IndexedAggregateRating.create();

  private EventSearch lastEventSearch;

  // TODO(avaliani): jackson doesn't like oAuth. It converts it to "oauth".
  private List<OAuthCredential> oauthCredentials = Lists.newArrayList();

  // TODO(avaliani): profileSecurityPrefs

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    // TODO(avaliani): ProfileImage is valuable for testing. Eventually null
    // this out.
    // profileImage = null;
    eventOrganizerRating = IndexedAggregateRating.create();
  }

  @Override
  protected void processUpdate(User oldUser) {
    super.processUpdate(oldUser);
    // Some fields can not be manipulated by updating the user.
    oauthCredentials = oldUser.getOauthCredentials();

    // Some fields are explicitly updated.
    profileImage = oldUser.profileImage;
    eventOrganizerRating = oldUser.eventOrganizerRating;

    // TODO(avlaiani): re-evaluate this. All fields should be updateable if you have admin
    //     privileges.
    // setKarmaPoints(oldUser.getKarmaPoints());
    // setEventOrganizerRating(oldUser.getEventOrganizerRating());
  }

  @Override
  protected void processDelete() {
    // TODO(avaliani):
    //   - revoke OAuth credentials. This way the user account won't be re-created
    //     automatically.
    //   - remove the user from all events that the user is a participant of.
    if (profileImage != null) {
      BaseDao.delete(KeyWrapper.toKey(profileImage.getImage()));
      profileImage = null;
    }
  }

  @Override
  protected void updatePermission() {
    if (Key.create(this).equals(getCurrentUserKey())) {
      setPermission(Permission.ALL);
    } else {
      setPermission(Permission.READ);
    }
  }

  private void updateProfileImage(@Nullable Image profileImage) {
    this.profileImage = (profileImage == null) ? null : ImageRef.create(profileImage);
  }

  public static User getUser(OAuthCredential credential) {
    return loadFirst(ofy().load().type(User.class)
      .filter("oauthCredentials.globalUid", credential.getGlobalUid()));
  }

  public static void updateProfileImage(Key<User> userKey, BlobKey blobKey) {
    ofy().transact(new UpdateProfileImageTxn(userKey, null, blobKey));
  }

  public static void updateProfileImage(Key<User> userKey, SocialNetworkProviderType providerType) {
    ofy().transact(new UpdateProfileImageTxn(userKey, providerType, null));
  }

  public static void deleteProfileImage(Key<User> userKey) {
    ofy().transact(new UpdateProfileImageTxn(userKey, null, null));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class UpdateProfileImageTxn extends VoidWork {
    private final Key<User> userKey;
    private final SocialNetworkProviderType imageProviderType;
    private final BlobKey blobKey;

    public void vrun() {
      User user = BaseDao.load(userKey);
      if (user == null) {
        throw ErrorResponseMsg.createException("user not found", ErrorInfo.Type.BAD_REQUEST);
      }
      if (!user.permission.canEdit()) {
        throw ErrorResponseMsg.createException(
          "insufficient privileges to edit user", ErrorInfo.Type.BAD_REQUEST);
      }

      Key<Image> existingImageKey = null;
      if (user.profileImage != null) {
        existingImageKey = KeyWrapper.toKey(user.profileImage.getImage());
        BaseDao.delete(existingImageKey);
      }

      Image newProfileImage;
      if (imageProviderType != null) {
        // Currently we only store the login credentials. In the future we should have access to
        // multiple credentials. When we do, this code should change.
        OAuthCredential credential = UserService.getCurrentUserCredential();
        SocialNetworkProviderType credentialProviderType =
            SocialNetworkProviderFactory.getProviderType(credential);
        if (credentialProviderType != imageProviderType) {
          throw ErrorResponseMsg.createException(
            "no credentials for provider type: " + imageProviderType,
            ErrorInfo.Type.BAD_REQUEST);
        }
        SocialNetworkProvider imageProvider = SocialNetworkProviderFactory.getProvider(credential);
        newProfileImage = Image.createAndPersist(userKey, imageProvider.getProfileImageUrl(),
          imageProvider.getProviderType());
      } else if (blobKey != null) {
        newProfileImage = Image.createAndPersist(userKey, blobKey, null);
      } else {
        newProfileImage = null;
      }
      user.updateProfileImage(newProfileImage);
      BaseDao.partialUpdate(user);

      if (existingImageKey != null) {
        ImageRef.updateRefs(existingImageKey,
          (newProfileImage == null) ? null : Key.create(newProfileImage));
      }
    }
  }

}
