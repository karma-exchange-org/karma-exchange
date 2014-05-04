package org.karmaexchange.dao;

import static com.google.common.base.Preconditions.checkState;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.validator.routines.EmailValidator;
import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.dao.Organization.Role;
import org.karmaexchange.resources.msg.AuthorizationErrorInfo;
import org.karmaexchange.resources.msg.BaseDaoView;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

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
public final class User extends IdBaseDao<User> implements BaseDaoView<User> {

  private static final int MAX_ATTENDANCE_HISTORY = 10;

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

  // TODO(avaliani): profileSecurityPrefs

  // NOTE: Embedded list is safe since OrganizationMembership has been modified to avoid
  //       encountering the objectify serialization bug (issue #127).
  private List<OrganizationMembership> organizationMemberships = Lists.newArrayList();

  // TODO(avaliani): cleanup post demo.
  private List<BadgeSummary> badges = Lists.newArrayList();

  public static User create() {
    User user = new User();
    user.setModificationInfo(ModificationInfo.create());
    return user;
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    initSearchableFullName();

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
  }

  @Override
  protected void processUpdate(User oldUser) {
    super.processUpdate(oldUser);
    // Some fields can not be manipulated by updating the user.
    initSearchableFullName();

    // Some fields are explicitly updated.
    profileImage = oldUser.profileImage;
    eventOrganizerRating = oldUser.eventOrganizerRating;
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

  public static Key<User> persistNewUser(UserInfo userInfo) {
    User user = userInfo.getUser();
    checkState(!user.isKeyComplete(), "new user can not have complete key");
    BaseDao.upsert(user);
    // TODO(avaliani): users should be in an orphaned state until they are attached

    if (userInfo.getProfileImageUrl() != null) {
      updateProfileImage(Key.create(user), userInfo.getProfileImageProvider(),
        userInfo.getProfileImageUrl());
    }

    return Key.create(user);
  }

  private void updateProfileImage(@Nullable Image profileImage) {
    this.profileImage = (profileImage == null) ? null : ImageRef.create(profileImage);
  }

  public static void updateProfileImage(Key<User> userKey, BlobKey blobKey) {
    ofy().transact(new UpdateProfileImageTxn(userKey, ImageProviderType.BLOBSTORE, null, blobKey));
  }

  public static void updateProfileImage(Key<User> userKey, ImageProviderType imageProviderType,
      String imageUrl) {
    ofy().transact(new UpdateProfileImageTxn(userKey, imageProviderType, imageUrl, null));
  }

  public static void deleteProfileImage(Key<User> userKey) {
    ofy().transact(new UpdateProfileImageTxn(userKey, null, null, null));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class UpdateProfileImageTxn extends VoidWork {
    private final Key<User> userKey;
    private final ImageProviderType imageProviderType;
    private final String imageUrl;
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
      setProfileImage(user, imageProviderType, imageUrl, blobKey);
      BaseDao.partialUpdate(user);
    }

  }

  private static void setProfileImage(User user, ImageProviderType imageProviderType,
      String imageUrl, BlobKey blobKey) {
    Key<User> userKey = Key.create(user);
    Key<Image> existingImageKey = null;
    if (user.profileImage != null) {
      existingImageKey = KeyWrapper.toKey(user.profileImage.getRef());
      BaseDao.delete(existingImageKey);
    }

    Image newProfileImage;
    if (imageProviderType == ImageProviderType.FACEBOOK) {
      newProfileImage = Image.createAndPersist(userKey, imageProviderType, imageUrl);
    } else if (imageProviderType == ImageProviderType.BLOBSTORE) {
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

  @XmlTransient
  public String getPrimaryEmail() {
    for (RegisteredEmail registeredEmail : getRegisteredEmails()) {
      if (registeredEmail.isPrimary()) {
        return registeredEmail.getEmail();
      }
    }
    return null;
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

  @Override
  public User getDao() {
    return this;
  }

  @Data
  public static class KarmaGoal {
    private static final long DEFAULT_MONTHLY_GOAL = 1 * 60;

    private long monthlyGoal = DEFAULT_MONTHLY_GOAL;
  }
}
