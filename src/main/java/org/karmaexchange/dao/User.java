package org.karmaexchange.dao;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.karmaexchange.provider.SocialNetworkProviderFactory.getProviderType;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.validator.routines.EmailValidator;
import org.karmaexchange.dao.Organization.Role;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.AuthorizationErrorInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;
import org.karmaexchange.util.AdminUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
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

  private static final int MAX_ATTENDANCE_HISTORY = 10;

  private static final SocialNetworkProviderType USER_KEY_PROVIDER =
      SocialNetworkProviderType.FACEBOOK;

  @Index
  private String firstName;
  @Index
  private String lastName;
  @Index
  private String searchableFullName;
  @Index
  private String nickName;

  private Gender gender;
  private AgeRange ageRange;

  private ImageRef profileImage;

  private List<RegisteredEmail> registeredEmails = Lists.newArrayList();
  private String phoneNumber;
  private Address address;

  // NOTE: Embedded list is safe since EmergencyContact has no embedded objects.
  private List<EmergencyContact> emergencyContacts = Lists.newArrayList();

  private String about;

  @Index
  private List<CauseType> causes = Lists.newArrayList();

  // Skipping interests for now.
  // Facebook has a detailed + categorized breakdown of interests.

  @Index
  private long karmaPoints;
  private KarmaGoal karmaGoal;

  private List<AttendanceRecord> eventAttendanceHistory = Lists.newArrayList();
  @Ignore
  private Double eventAttendanceHistoryPct;

  private IndexedAggregateRating eventOrganizerRating;

  private EventSearch lastEventSearch;

  // TODO(avaliani): jackson doesn't like oAuth. It converts it to "oauth".
  // NOTE: Embedded list is safe since OAuthCredential has no embedded objects.
  private List<OAuthCredential> oauthCredentials = Lists.newArrayList();

  // TODO(avaliani): profileSecurityPrefs

  // NOTE: Embedded list is safe since OrganizationMembership has been modified to avoid
  //       encountering the objectify serialization bug (issue #127).
  private List<OrganizationMembership> organizationMemberships = Lists.newArrayList();

  // TODO(avaliani): cleanup post demo.
  private List<BadgeSummary> badges = Lists.newArrayList();

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
    initSearchableFullName();
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
    if (karmaGoal == null) {
      karmaGoal = new KarmaGoal();
    }
    eventAttendanceHistory = Lists.newArrayList();
    organizationMemberships = Lists.newArrayList();

    validateUser();
  }

  @Override
  protected void postProcessInsert() {
    super.postProcessInsert();
    UserUsage.trackUser(this);
  }

  @Override
  protected void processUpdate(User oldUser) {
    super.processUpdate(oldUser);
    // Some fields can not be manipulated by updating the user.
    initSearchableFullName();

    // Some fields are explicitly updated.
    profileImage = oldUser.profileImage;
    eventOrganizerRating = oldUser.eventOrganizerRating;
    oauthCredentials = Lists.newArrayList(oldUser.oauthCredentials);
    karmaPoints = oldUser.karmaPoints;
    eventAttendanceHistory = oldUser.eventAttendanceHistory;
    organizationMemberships = oldUser.organizationMemberships;

    validateUser();
  }

  @Override
  public void processLoad() {
    super.processLoad();
    updateEventAttendanceHistoryPct();
  }

  private void validateUser() {
    List<ValidationError> validationErrors = Lists.newArrayList();

    boolean primaryEmailFound = false;
    for (RegisteredEmail registeredEmail : registeredEmails) {
      validationErrors.addAll(registeredEmail.validate(this));
      if (registeredEmail.isPrimary) {
        if (primaryEmailFound) {
          // Multiple primary emails.
          validationErrors.add(new ResourceValidationError(
            this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID,
            "registeredEmails.isPrimary"));
        }
        primaryEmailFound = true;
      }
    }
    if (!registeredEmails.isEmpty() && !primaryEmailFound) {
      // Emails registered but no primary email.
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID,
        "registeredEmails.isPrimary"));
    }

    if (!validationErrors.isEmpty()) {
      throw ValidationErrorInfo.createException(validationErrors);
    }
  }

  private void initSearchableFullName() {
    if (searchableFullName == null) {
      searchableFullName = firstName + " " + lastName;
    }
    searchableFullName = searchableFullName.toLowerCase();
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

  public static User persistNewUser(User user) {
    PersistNewUserTxn persistNewUserTxn = new PersistNewUserTxn(user);
    ofy().transact(persistNewUserTxn);
    return persistNewUserTxn.user;
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  @AllArgsConstructor
  private static class PersistNewUserTxn extends VoidWork {
    private User user;

    public void vrun() {
      User existingUser = ofy().load().key(Key.create(user)).now();
      // Don't wipe out an existing user object. State like karma points, etc. should be
      // retained.
      if (existingUser == null) {
        User.bootstrapProfileImage(user);
        BaseDao.upsert(user);
      } else {
        user = existingUser;
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
      User user = ofy().load().key(userKey).now();
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

  @Embed
  @Data
  @NoArgsConstructor
  public static final class OrganizationMembership {
    private KeyWrapper<Organization> organization;
    @Nullable
    private Organization.Role role;
    @Nullable
    private Organization.Role requestedRole;

    // Added to enable querying
    @Index
    @Nullable
    private NullableKeyWrapper<Organization> organizationMember =
        NullableKeyWrapper.create();
    @Index
    @Nullable
    private NullableKeyWrapper<Organization> organizationMemberWithAdminRole =
        NullableKeyWrapper.create();
    @Index
    @Nullable
    private NullableKeyWrapper<Organization> organizationMemberWithOrganizerRole =
        NullableKeyWrapper.create();

    @Index
    @Nullable
    private NullableKeyWrapper<Organization> organizationPendingMembershipRequest =
        NullableKeyWrapper.create();

    public OrganizationMembership(Key<Organization> orgKey,
        @Nullable Organization.Role grantedRole, @Nullable Organization.Role requestedRole) {
      organization = KeyWrapper.create(orgKey);
      this.role = grantedRole;
      this.requestedRole = requestedRole;

      if (grantedRole != null) {
        organizationMember = NullableKeyWrapper.create(orgKey);
        if (grantedRole == Role.ADMIN) {
          organizationMemberWithAdminRole = NullableKeyWrapper.create(orgKey);
        } else if (grantedRole == Role.ORGANIZER) {
          organizationMemberWithOrganizerRole = NullableKeyWrapper.create(orgKey);
        }
      }

      if (requestedRole != null) {
        organizationPendingMembershipRequest = NullableKeyWrapper.create(orgKey);
      }
    }

    public static Predicate<OrganizationMembership> userPredicate(final Key<Organization> orgKey) {
      return new Predicate<OrganizationMembership>() {
        @Override
        public boolean apply(@Nullable OrganizationMembership input) {
          return KeyWrapper.toKey(input.organization).equals(orgKey);
        }
      };
    }
  }

  public boolean hasOrgMembership(Key<Organization> org, Organization.Role role) {
    OrganizationMembership membership = tryFindOrganizationMembership(org);
    return (membership != null) && (membership.role != null) &&
        membership.role.hasEqualOrMoreCapabilities(role);
  }

  @Nullable
  public OrganizationMembership tryFindOrganizationMembership(Key<Organization> orgKey) {
    return Iterables.tryFind(organizationMemberships, OrganizationMembership.userPredicate(orgKey))
        .orNull();
  }

  public static void updateMembership(Key<User> userToUpdateKey, Key<Organization> organizationKey,
      @Nullable Organization.Role role) {
    Organization org = ofy().load().key(organizationKey).now();
    if (org == null) {
      throw ErrorResponseMsg.createException("org not found", ErrorInfo.Type.BAD_REQUEST);
    }
    ofy().transact(new UpdateMembershipTxn(
      userToUpdateKey, org, org.isCurrentUserOrgAdmin(), role));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class UpdateMembershipTxn extends VoidWork {
    private final Key<User> userToUpdateKey;
    private final Organization organization;
    private final boolean currentUserIsOrgAdmin;
    @Nullable
    private final Organization.Role reqRole;

    public void vrun() {
      User user = ofy().load().key(userToUpdateKey).now();
      if (user == null) {
        throw ErrorResponseMsg.createException("user not found", ErrorInfo.Type.BAD_REQUEST);
      }

      OrganizationMembership existingMembership =
          user.tryFindOrganizationMembership(Key.create(organization));

      RequestStatus membershipStatus = null;
      if (currentUserIsOrgAdmin) {
        // TODO(avaliani): If the current user is an org admin and the target user is not a member
        //     of the org and has not requested to be a member of the org, we should validate
        //     via email that the user wants to join the org.
        membershipStatus = RequestStatus.ACCEPTED;
      } else {
        if (reqRole == null) {
          // Delete membership.
          if (!getCurrentUserKey().equals(userToUpdateKey)) {
            throw AuthorizationErrorInfo.createException(userToUpdateKey);
          }
        } else {
          // Add / modify role.
          membershipStatus = RequestStatus.PENDING;
          if ((existingMembership != null) &&
              (existingMembership.role != null) &&
              (existingMembership.role.hasEqualOrMoreCapabilities(reqRole))) {
            membershipStatus = RequestStatus.ACCEPTED;
          } else {
            for (RegisteredEmail registeredEmail : user.registeredEmails) {
              if (organization.canAutoGrantMembership(registeredEmail.email, reqRole)) {
                membershipStatus = RequestStatus.ACCEPTED;
                break;
              }
            }
          }
        }
      }

      // First remove any existing membership.
      if (existingMembership != null) {
        user.organizationMemberships.remove(existingMembership);
      }
      // Then add the new role if any.
      if (reqRole != null) {
        OrganizationMembership membershipToUpsert;
        if (membershipStatus == RequestStatus.ACCEPTED) {
          membershipToUpsert = new OrganizationMembership(Key.create(organization), reqRole, null);
        } else {
          membershipToUpsert = new OrganizationMembership(Key.create(organization),
            (existingMembership == null) ? null : existingMembership.role, reqRole);
        }
        user.organizationMemberships.add(membershipToUpsert);
      }

      // Persist the changes.
      BaseDao.partialUpdate(user);
    }
  }

  @Embed
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisteredEmail {
    private String email;
    private boolean isPrimary;
    // TODO(avaliani): private boolean verified

    public List<ValidationError> validate(User user) {
      List<ValidationError> validationErrors = Lists.newArrayList();
      if (!EmailValidator.getInstance().isValid(email)) {
        validationErrors.add(new ResourceValidationError(
          user, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID,
          "registeredEmails[email=\"" + email + "\"]"));
      }
      return validationErrors;
    }
  }

  public void removeFromEventAttendanceHistory(Key<Event> eventKey) {
    Iterables.removeIf(eventAttendanceHistory, AttendanceRecord.eventPredicate(eventKey));
  }

  public void addToAttendanceHistory(AttendanceRecord newRec) {
    eventAttendanceHistory.add(newRec);
    Collections.sort(eventAttendanceHistory, AttendanceRecord.EventStartTimeComparator.INSTANCE);
    if (eventAttendanceHistory.size() > MAX_ATTENDANCE_HISTORY) {
      eventAttendanceHistory.subList(MAX_ATTENDANCE_HISTORY, eventAttendanceHistory.size()).clear();
    }
  }

  public void updateEventAttendanceHistoryPct() {
    int eventsAttended = 0;
    for (AttendanceRecord rec : eventAttendanceHistory) {
      if (rec.isAttended()) {
        eventsAttended++;
      }
    }
    eventAttendanceHistoryPct = ((double) eventsAttended) / eventAttendanceHistory.size() * 100;
  }

  @Embed
  @Data
  public static class KarmaGoal {
    private static final long DEFAULT_MONTHLY_GOAL = 1 * 60;

    private long monthlyGoal = DEFAULT_MONTHLY_GOAL;
  }
}
