package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public final class Event extends BaseDao<Event> {

  public static final int MAX_CACHED_PARTICIPANT_IMAGES = 10;

  /*
   * TODO(avaliani):
   *   - look at volunteer match schema
   *   - compare this to Meetup, OneBrick, Golden Gate athletic club, etc.
   */

  @Id private Long id;
  @Ignore
  private String key;
  private ModificationInfo modificationInfo;

  @Ignore
  private Permission permission;

  private String title;
  private String description;
  private String specialInstructions; // See flash volunteer.
  @Index
  private List<KeyWrapper<Cause>> causes = Lists.newArrayList();

  private Location location;
  @Index
  private Date startTime;
  @Index
  private Date endTime;

  private Image primaryImage;
  private List<Image> allImages = Lists.newArrayList();

  @Index
  private List<KeyWrapper<Skill>> skillsPreferred = Lists.newArrayList();
  @Index
  private List<KeyWrapper<Skill>> skillsRequired = Lists.newArrayList();

  // Organizations can co-host events. Admins of the organization can edit the event.
  @Index
  private List<KeyWrapper<Organization>> organizations = Lists.newArrayList();
  // Organizers can also edit the event.
  @Index
  private List<KeyWrapper<User>> organizers = Lists.newArrayList();

  // This field is automatically managed.
  @Index
  private Status status;

  private int minRegistrations;
  // The maxRegistration limit only applies to participants. The limit does not include organizers.
  private int maxRegistrations;
  private int maxWaitingList;
  @Index
  private List<KeyWrapper<User>> registeredUsers = Lists.newArrayList();
  private List<KeyWrapper<User>> waitingListUsers = Lists.newArrayList();

  // This field is automatically managed.
  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private Rating eventRating;

  /**
   * The number of karma points earned by participating in the event.
   */
  @Index
  private int karmaPoints;

  public enum Status {
    OPEN,
    FULL,
    COMPLETED
    /* HOLD, */  // Edit only. Not visible for public search.
    /* CANCELED */
  }

  @Override
  public void setId(Long id) {
    this.id = id;
    updateKey();
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    validateEvent();
    // Ignore anything that was explicitly passed in.
    cachedParticipantImages = Lists.newArrayList();
    updateCachedParticipantImages();
    updateStatus();
    if (getOrganizers().isEmpty()) {
      KeyWrapper<User> currentUserKey = KeyWrapper.create(getCurrentUserKey());
      getOrganizers().add(currentUserKey);
      getRegisteredUsers().remove(currentUserKey);
      getWaitingListUsers().remove(currentUserKey);
    }
  }

  @Override
  protected void processUpdate(Event prevObj) {
    super.processUpdate(prevObj);
    validateEvent();
    updateRegisteredUsers();
    updateCachedParticipantImages();
    updateStatus();
  }

  private void validateEvent() {
    if (null == getStartTime()) {
      throw ErrorResponseMsg.createException("the event start time can not be null",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (null == getEndTime()) {
      throw ErrorResponseMsg.createException("the event end time can not be null",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (getEndTime().before(getStartTime())) {
      throw ErrorResponseMsg.createException(
        "the event end time must be after the event start time",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (getMinRegistrations() > getMaxRegistrations()) {
      throw ErrorResponseMsg.createException(
        "the minimum number of registrations must be less than or equal to the maximum number" +
            "of registrations",
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  private void updateRegisteredUsers() {
    if ((getRegisteredUsers().size() < getMaxRegistrations()) &&
        !getWaitingListUsers().isEmpty()) {
      int numSpots = getMaxRegistrations() - getRegisteredUsers().size();
      List<KeyWrapper<User>> usersToRegister =
          getWaitingListUsers().subList(0, Math.min(numSpots, getWaitingListUsers().size()));
      getRegisteredUsers().addAll(usersToRegister);
      usersToRegister.clear();
    }
  }

  private void updateCachedParticipantImages() {
    int numParticipantImagesToCache = getNumParticipants() - getCachedParticipantImages().size();
    numParticipantImagesToCache = Math.min(numParticipantImagesToCache,
      MAX_CACHED_PARTICIPANT_IMAGES - getCachedParticipantImages().size());
    if (numParticipantImagesToCache > 0) {
      List<Key<User>> usersToFetch = Lists.newArrayList();
      for (KeyWrapper<User> participantKey : getParticipantKeys(MAX_CACHED_PARTICIPANT_IMAGES)) {
        if (!Iterables.any(getCachedParticipantImages(),
            ParticipantImage.createEqualityPredicate(participantKey))) {
          usersToFetch.add(KeyWrapper.toKey(participantKey));
          numParticipantImagesToCache--;
          if (numParticipantImagesToCache == 0) {
            break;
          }
        }
      }
      List<User> participantsToCache = BaseDao.load(usersToFetch, true);
      for (User participantToCache : participantsToCache) {
        getCachedParticipantImages().add(ParticipantImage.create(participantToCache));
      }
    }
  }

  private void updateStatus() {
    Date now = new Date();
    if (getEndTime().before(now)) {
      setStatus(Status.COMPLETED);
    } else if (getRegisteredUsers().size() < getMaxRegistrations()) {
      setStatus(Status.OPEN);
    } else {
      setStatus(Status.FULL);
    }
  }

  private void deleteParticipant(Key<User> userKey) {
    getOrganizers().remove(KeyWrapper.create(userKey));
    getRegisteredUsers().remove(KeyWrapper.create(userKey));
    Iterables.removeIf(getCachedParticipantImages(),
      ParticipantImage.createEqualityPredicate(KeyWrapper.create(userKey)));
  }

  private List<KeyWrapper<User>> getParticipantKeys(int limit) {
    List<KeyWrapper<User>> participants = Lists.newArrayList();
    for (KeyWrapper<User> organizer : getOrganizers()) {
      participants.add(organizer);
      limit--;
      if (limit == 0) {
        break;
      }
    }
    if (limit > 0) {
      for (KeyWrapper<User> registeredUser : getRegisteredUsers()) {
        participants.add(registeredUser);
        limit--;
        if (limit == 0) {
          break;
        }
      }
    }
    return participants;
  }

  public int getNumParticipants() {
    return getOrganizers().size() + getRegisteredUsers().size();
  }

  @Override
  protected void updatePermission() {
    if (getOrganizations().isEmpty()) {
      if (organizers.contains(KeyWrapper.create(getCurrentUserKey()))) {
        setPermission(Permission.ALL);
      } else {
        setPermission(Permission.READ);
      }
    } else {
      // List<Organization> organizations = BaseDao.load(KeyWrapper.toKeys(getOrganizations()));
      // TODO(avaliani): fill in when we convert organizations to BaseDao.
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class AddRegisteredUserTxn extends VoidWork {
    private final Key<Event> eventKey;
    private final Key<User> userKey;

    public void vrun() {
      Event event = load(eventKey);
      if (event == null) {
        throw ErrorResponseMsg.createException("event not found",
          ErrorInfo.Type.BAD_REQUEST);
      }
      // If the user is already part of the event or an organizer, do not add the user.
      KeyWrapper<User> wrappedUserKey = KeyWrapper.create(userKey);
      if (event.getRegisteredUsers().contains(wrappedUserKey) ||
          event.getOrganizers().contains(wrappedUserKey)) {
        return;
      }
      if (event.getRegisteredUsers().size() >= event.getMaxRegistrations()) {
        throw ErrorResponseMsg.createException("the event has reached the max registration limit",
          ErrorInfo.Type.LIMIT_REACHED);
      }
      event.getRegisteredUsers().add(wrappedUserKey);
      event.getWaitingListUsers().remove(wrappedUserKey);
      event.update(event);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class DeleteRegisteredUserTxn extends VoidWork {
    private final Key<Event> eventKey;
    private final Key<User> userKey;

    public void vrun() {
      Event event = load(eventKey);
      if (event == null) {
        throw ErrorResponseMsg.createException("event not found",
          ErrorInfo.Type.BAD_REQUEST);
      }
      event.deleteParticipant(userKey);
      event.update(event);
    }
  }
}
