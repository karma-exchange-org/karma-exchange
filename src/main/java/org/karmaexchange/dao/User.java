package org.karmaexchange.dao;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.karmaexchange.provider.SocialNetworkProviderFactory.getProviderType;
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
import org.karmaexchange.util.AdminUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.common.collect.Iterables;
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
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class User extends NameBaseDao<User> {

  private static final SocialNetworkProviderType USER_KEY_PROVIDER =
      SocialNetworkProviderType.FACEBOOK;

  @Index
  private String firstName;
  @Index
  private String lastName;
  @Index
  private String nickName;

  private Gender gender;
  private AgeRange ageRange;

  private ImageRef profileImage;
  private ContactInfo contactInfo;
  private List<EmergencyContact> emergencyContacts = Lists.newArrayList();

  private String about;

  @Index
  private List<KeyWrapper<CauseType>> causes = Lists.newArrayList();

  @Index
  private List<KeyWrapper<Skill>> skills = Lists.newArrayList();

  // Skipping interests for now.
  // Facebook has a detailed + categorized breakdown of interests.

  @Index
  private long karmaPoints;

  private IndexedAggregateRating eventOrganizerRating;

  private EventSearch lastEventSearch;

  // TODO(avaliani): jackson doesn't like oAuth. It converts it to "oauth".
  private List<OAuthCredential> oauthCredentials = Lists.newArrayList();

  // TODO(avaliani): profileSecurityPrefs

  public static User create(OAuthCredential credential) {
    return new User(credential);
  }

  private User(OAuthCredential credential) {
    oauthCredentials.add(credential);
    initKey();
    setModificationInfo(ModificationInfo.create());
  }

  public void initKey() {
    owner = null;
    name = getKeyProviderCredential().getGlobalUid();
  }

  private OAuthCredential getKeyProviderCredential() {
    for (OAuthCredential credential : oauthCredentials) {
      if (getProviderType(credential) == USER_KEY_PROVIDER) {
        return credential;
      }
    }
    throw ErrorResponseMsg.createException(
      format("%s oauth provider credential required for user object", USER_KEY_PROVIDER),
      ErrorInfo.Type.BAD_REQUEST);
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();

    if (AdminUtil.isAdminKey(Key.create(this))) {
      throw ErrorResponseMsg.createException(
        "can not use reserved admin key as user object key: " + Key.create(this),
        ErrorInfo.Type.BAD_REQUEST);
    }

    // TODO(avaliani): ProfileImage is valuable for testing. Eventually null
    // this out.
    // profileImage = null;
    eventOrganizerRating = IndexedAggregateRating.create();
    karmaPoints = 0;
  }

  @Override
  protected void processUpdate(User oldUser) {
    super.processUpdate(oldUser);
    // Some fields can not be manipulated by updating the user.
    oauthCredentials = oldUser.getOauthCredentials();

    // Some fields are explicitly updated.
    profileImage = oldUser.profileImage;
    eventOrganizerRating = oldUser.eventOrganizerRating;
    oauthCredentials = Lists.newArrayList(oldUser.oauthCredentials);
    karmaPoints = oldUser.karmaPoints;
  }

  @Override
  protected void processDelete() {
    // TODO(avaliani):
    //   - revoke OAuth credentials. This way the user account won't be re-created
    //     automatically.
    //   - remove the user from all events that the user is a participant of.
    if (profileImage != null) {
      BaseDao.delete(KeyWrapper.toKey(profileImage.getRef()));
      profileImage = null;
    }
  }

  @Override
  protected Permission evalPermission() {
    if (Key.create(this).equals(getCurrentUserKey())) {
      return Permission.ALL;
    } else {
      return Permission.READ;
    }
  }

  public static void persistNewUser(User user) {
    ofy().transact(new PersistNewUserTxn(user));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class PersistNewUserTxn extends VoidWork {
    private final User user;

    public void vrun() {
      User existingUser = BaseDao.load(Key.create(user));
      // Don't wipe out an existing user object. State like karma points, etc. should be
      // retained.
      if (existingUser == null) {
        User.bootstrapProfileImage(user);
        BaseDao.upsert(user);
      }
    }
  }

  private void updateProfileImage(@Nullable Image profileImage) {
    this.profileImage = (profileImage == null) ? null : ImageRef.create(profileImage);
  }

  public static Key<User> getKey(OAuthCredential credential) {
    checkState(getProviderType(credential) == USER_KEY_PROVIDER,
        format("provider[%s] does not match user key provider[%s]",
          getProviderType(credential), USER_KEY_PROVIDER));
    return Key.create(User.class, credential.getGlobalUid());
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
      setProfileImage(user, imageProviderType, blobKey);
      BaseDao.partialUpdate(user);
    }

  }

  // Must be called from within a transaction.
  public static void bootstrapProfileImage(User user) {
    setProfileImage(user, USER_KEY_PROVIDER, null);
  }

  private static void setProfileImage(User user, SocialNetworkProviderType imageProviderType,
      BlobKey blobKey) {
    Key<User> userKey = Key.create(user);
    Key<Image> existingImageKey = null;
    if (user.profileImage != null) {
      existingImageKey = KeyWrapper.toKey(user.profileImage.getRef());
      BaseDao.delete(existingImageKey);
    }

    Image newProfileImage;
    if (imageProviderType != null) {
      OAuthCredential credential = Iterables.tryFind(
        user.oauthCredentials, OAuthCredential.providerPredicate(imageProviderType)).orNull();
      if (credential == null) {
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

    if (existingImageKey != null) {
      ImageRef.updateRefs(existingImageKey,
        (newProfileImage == null) ? null : Key.create(newProfileImage));
    }
  }
}
