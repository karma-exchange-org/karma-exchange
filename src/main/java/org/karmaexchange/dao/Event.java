package org.karmaexchange.dao;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;
import static com.google.common.base.Preconditions.checkState;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.task.ProcessOrganizerRatingsServlet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;

// TODO(avaliani):
//   - Fix EventSearchView for images once we revamp it.
@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class Event extends IdBaseDao<Event> {

  public static final int MAX_EVENT_KARMA_POINTS = 500;

  public static final int MAX_CACHED_PARTICIPANT_IMAGES = 10;

  /*
   * TODO(avaliani):
   *   - look at volunteer match schema
   *   - compare this to Meetup, OneBrick, Golden Gate athletic club, etc.
   */

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

  // Can not be explicitly set. Automatically managed.
  @Ignore
  private List<KeyWrapper<User>> organizers = Lists.newArrayList();

  // Can not be explicitly set. Automatically managed.
  @Ignore
  private RegistrationInfo registrationInfo;

  private int minRegistrations;
  // The maxRegistration limit only applies to participants. The limit does not include organizers.
  private int maxRegistrations;
  private int maxWaitingList;

  // Can not be explicitly set. Automatically managed.
  @Ignore
  private List<KeyWrapper<User>> registeredUsers = Lists.newArrayList();
  // Can not be explicitly set. Automatically managed.
  @Ignore
  private List<KeyWrapper<User>> waitListedUsers = Lists.newArrayList();

  // We need a consolidated list because pagination does not support OR queries.
  @Index
  private List<EventParticipant> participants = Lists.newArrayList();

  // Can not be set. Automatically managed. Only includes organizers and registered users. Wait
  // listed users images are skipped.
  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private IndexedAggregateRating rating;
  private DerivedOrganizerRatings derivedOrganizerRatings;

  /**
   * The number of karma points earned by participating in the event. This is derived from the
   * start and end time.
   */
  @Index
  private int karmaPoints;

  public enum RegistrationInfo {
    ORGANIZER,
    REGISTERED,
    WAIT_LISTED,
    CAN_REGISTER,
    CAN_WAIT_LIST,
    FULL
  }

  public enum ParticipantType {
    ORGANIZER,
    REGISTERED,
    WAIT_LISTED
  }

  public void setOrganizers(List<KeyWrapper<User>> ignored) {
    // No-op it.
  }
  public void setRegisteredUsers(List<KeyWrapper<User>> ignored) {
    // No-op it.
  }
  public void setWaitListedUsers(List<KeyWrapper<User>> ignored) {
    // No-op it.
  }
  public void setCachedParticipantImages(List<ParticipantImage> ignored) {
    // No-op it.
  }
  public void setRegistrationInfo(RegistrationInfo ignored) {
    // No-op it.
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    initParticipantLists();
    // Add the current user as an organizer if there are no organizers registered.
    if (organizers.isEmpty()) {
      Iterables.removeIf(participants, EventParticipant.userPredicate(getCurrentUserKey()));
      participants.add(EventParticipant.create(getCurrentUserKey(), ParticipantType.ORGANIZER));
      initParticipantLists();
    }
    processParticipants();
    validateEvent();
    initKarmaPoints();
    rating = IndexedAggregateRating.create();
    initDerivedOrganizerRatings();
  }

  @Override
  protected void processUpdate(Event prevObj) {
    super.processUpdate(prevObj);
    // Karma points are set when the event is created.
    karmaPoints = prevObj.karmaPoints;
    // Rating is independently and transactionally updated.
    rating = prevObj.rating;
    derivedOrganizerRatings = prevObj.derivedOrganizerRatings;
    // Participants is independently and transactionally updated.
    participants = Lists.newArrayList(prevObj.participants);
    processParticipants();
    validateEvent();
  }

  private void processParticipantMutation(EventParticipant updatedParticipant, boolean wasDeleted) {
    processParticipants();
    derivedOrganizerRatings.processParticipantMutation(this, updatedParticipant, wasDeleted);
    validateEvent();
  }

  private void processRatingUpdate() {
    derivedOrganizerRatings.processRatingUpdate(this);
  }

  private void processPendingRating() {
    derivedOrganizerRatings.processPendingRating(this);
  }

  private boolean hasPendingRatings() {
    return derivedOrganizerRatings.hasPendingRatings();
  }

  private void processParticipants() {
    initParticipantLists();
    processWaitList();
    updateCachedParticipantImages();
  }

  @Override
  protected void processLoad() {
    // initParticipantLists() must be called prior to processLoad so that updatePermissions can
    // use the participant lists to calculate the permissions.
    initParticipantLists();
    super.processLoad();
    updateRegistrationInfo();
  }

  private void initKarmaPoints() {
    long eventDurationMins = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
    karmaPoints = (int) Math.min(eventDurationMins, MAX_EVENT_KARMA_POINTS);
  }

  private void initDerivedOrganizerRatings() {
    derivedOrganizerRatings = new DerivedOrganizerRatings();
    for (EventParticipant participant : participants) {
      if (participant.getType() == ParticipantType.ORGANIZER) {
        derivedOrganizerRatings.processParticipantMutation(this, participant, false);
      }
    }
  }

  private void initParticipantLists() {
    organizers = Lists.newArrayList();
    registeredUsers = Lists.newArrayList();
    waitListedUsers = Lists.newArrayList();;
    for (EventParticipant participant : participants) {
      switch (participant.getType()) {
        case ORGANIZER:
          organizers.add(participant.getUser());
          break;
        case REGISTERED:
          registeredUsers.add(participant.getUser());
          break;
        case WAIT_LISTED:
          waitListedUsers.add(participant.getUser());
          break;
        default:
          checkState(false, "unknown participant type: " + participant.getType());
      }
    }
  }

  @Nullable
  private EventParticipant tryFindParticipant(Key<User> userKey) {
    return Iterables.tryFind(participants, EventParticipant.userPredicate(userKey)).orNull();
  }

  public List<KeyWrapper<User>> getParticipants(ParticipantType paticipantType) {
    switch (paticipantType) {
      case ORGANIZER:
        return organizers;
      case REGISTERED:
        return registeredUsers;
      case WAIT_LISTED:
        return waitListedUsers;
      default:
        throw new IllegalStateException("unknown participant type: " + paticipantType);
    }
  }

  private void validateEvent() {
    if ((null == title) || title.isEmpty()) {
      throw ErrorResponseMsg.createException("all events require an event title",
        ErrorInfo.Type.BAD_REQUEST);
    }
    // TODO(avaliani): make sure the description has a minimum length. Avoiding this for now
    //   since we don't have auto event creation.
    if (null == startTime) {
      throw ErrorResponseMsg.createException("the event start time can not be null",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (null == endTime) {
      throw ErrorResponseMsg.createException("the event end time can not be null",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (endTime.before(startTime)) {
      throw ErrorResponseMsg.createException(
        "the event end time must be after the event start time",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (maxRegistrations < 1) {
      throw ErrorResponseMsg.createException(
        "max registrations must be greater than or equal to one",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (minRegistrations > maxRegistrations) {
      throw ErrorResponseMsg.createException(
        "the minimum number of registrations must be less than or equal to the maximum number" +
            "of registrations",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (organizers.isEmpty()) {
      throw ErrorResponseMsg.createException(
        "at least one organizer must be assigned to the event",
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  private void processWaitList() {
    if ((registeredUsers.size() < maxRegistrations) &&
        !waitListedUsers.isEmpty()) {
      int numSpots = maxRegistrations - registeredUsers.size();
      List<KeyWrapper<User>> usersToRegister =
          waitListedUsers.subList(0, Math.min(numSpots, waitListedUsers.size()));
      for (KeyWrapper<User> userToRegister : usersToRegister) {
        EventParticipant participant = Iterables.find(participants,
          EventParticipant.userPredicate(KeyWrapper.toKey(userToRegister)));
        participant.setType(ParticipantType.REGISTERED);
      }
      initParticipantLists();
    }
  }

  private void updateCachedParticipantImages() {
    // Prune any deleted or wait listed participants.
    Iterator<ParticipantImage> cachedImageIter = cachedParticipantImages.iterator();
    while (cachedImageIter.hasNext()) {
      ParticipantImage cachedImage = cachedImageIter.next();
      Key<User> userKey = KeyWrapper.toKey(cachedImage.getParticipant());
      EventParticipant participant = tryFindParticipant(userKey);
      if ((participant == null) || (participant.getType() == ParticipantType.WAIT_LISTED)) {
        cachedImageIter.remove();
      }
    }

    // Add images if there is room.
    int numParticipantImagesToCache = getNumAttending() - cachedParticipantImages.size();
    numParticipantImagesToCache = Math.min(numParticipantImagesToCache,
      MAX_CACHED_PARTICIPANT_IMAGES - cachedParticipantImages.size());
    if (numParticipantImagesToCache > 0) {
      List<Key<User>> usersToFetch = Lists.newArrayList();
      for (KeyWrapper<User> participantKey :
           getAttendingParticipants(MAX_CACHED_PARTICIPANT_IMAGES)) {
        if (!Iterables.any(cachedParticipantImages,
            ParticipantImage.userPredicate(participantKey))) {
          usersToFetch.add(KeyWrapper.toKey(participantKey));
          numParticipantImagesToCache--;
          if (numParticipantImagesToCache == 0) {
            break;
          }
        }
      }
      List<User> participantsToCache = BaseDao.load(usersToFetch, true);
      for (User participantToCache : participantsToCache) {
        cachedParticipantImages.add(ParticipantImage.create(participantToCache));
      }
    }
  }

  private List<KeyWrapper<User>> getAttendingParticipants(int limit) {
    List<KeyWrapper<User>> attendingParticipants = Lists.newArrayList();
    for (KeyWrapper<User> organizer : organizers) {
      attendingParticipants.add(organizer);
      limit--;
      if (limit == 0) {
        break;
      }
    }
    if (limit > 0) {
      for (KeyWrapper<User> registeredUser : registeredUsers) {
        attendingParticipants.add(registeredUser);
        limit--;
        if (limit == 0) {
          break;
        }
      }
    }
    return attendingParticipants;
  }

  public int getNumAttending() {
    return organizers.size() + registeredUsers.size();
  }

  public void setNumAttending(int ignore) {
    // No-op.
  }

  private void updateRegistrationInfo() {
    EventParticipant participant = tryFindParticipant(getCurrentUserKey());
    if (participant == null) {
      if (registeredUsers.size() < maxRegistrations) {
        registrationInfo = RegistrationInfo.CAN_REGISTER;
      } else if (waitListedUsers.size() < maxWaitingList) {
        registrationInfo = RegistrationInfo.CAN_WAIT_LIST;
      } else {
        registrationInfo = RegistrationInfo.FULL;
      }
    } else {
      if (participant.type == ParticipantType.ORGANIZER) {
        registrationInfo = RegistrationInfo.ORGANIZER;
      } else if (participant.type == ParticipantType.REGISTERED) {
        registrationInfo = RegistrationInfo.REGISTERED;
      } else {
        checkState(participant.type == ParticipantType.WAIT_LISTED,
            "unknown participant type: " + participant.type);
        registrationInfo = RegistrationInfo.WAIT_LISTED;
      }
    }
  }

  @Override
  protected void updatePermission() {
    if (organizations.isEmpty()) {
      if (isOrganizer(getCurrentUserKey())) {
        permission = Permission.ALL;
      } else {
        permission = Permission.READ;
      }
    } else {
      // List<Organization> organizations = BaseDao.load(KeyWrapper.toKeys(organizations));
      // TODO(avaliani): fill in when we convert organizations to BaseDao.
    }
  }

  private boolean isOrganizer(Key<User> userKey) {
    return organizers.contains(KeyWrapper.create(userKey));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class UpsertParticipantTxn extends VoidWork {
    private final Key<Event> eventKey;
    private final Key<User> userToUpsertKey;
    private final ParticipantType participantType;

    public void vrun() {
      Event event = BaseDao.load(eventKey);
      if (event == null) {
        throw ErrorResponseMsg.createException("event not found",
          ErrorInfo.Type.BAD_REQUEST);
      }

      EventParticipant participantToUpsert = event.tryFindParticipant(userToUpsertKey);
      // Users that can not edit the event are restricted to only adding themselves to the
      // registered list or the waiting list. Additionally, users with edit permissions
      // bypass all event registration and waiting list limits.
      if (!event.permission.canEdit()) {
        if (!userToUpsertKey.equals(getCurrentUserKey())) {
          throw ErrorResponseMsg.createException(
            "only organizers can add any participant to the event",
            ErrorInfo.Type.BAD_REQUEST);
        }
        if (participantType == ParticipantType.ORGANIZER) {
          throw ErrorResponseMsg.createException(
            "insufficent priveleges to make the current user an organizer of the event",
            ErrorInfo.Type.BAD_REQUEST);
        }
        if ((participantToUpsert != null) && (participantToUpsert.getType() == participantType)) {
          // Nothing to do.
          return;
        }
        if (participantType == ParticipantType.REGISTERED) {
          if (event.registeredUsers.size() >= event.maxRegistrations) {
            throw ErrorResponseMsg.createException(
              "the event has reached the max registration limit",
              ErrorInfo.Type.LIMIT_REACHED);
          }
        } else {
          checkState(participantType == ParticipantType.WAIT_LISTED);
          if (event.waitListedUsers.size() >= event.maxWaitingList) {
            throw ErrorResponseMsg.createException(
              "the event has reached the max waiting list limit",
              ErrorInfo.Type.LIMIT_REACHED);
          }
        }
      }
      if (participantToUpsert == null) {
        // TODO(avaliani): organizers should not in theory be allowed to add arbitrary users.
        //     However, this simplifies testing so we're going to allow it for now.
        participantToUpsert = EventParticipant.create(userToUpsertKey, participantType);
        event.participants.add(participantToUpsert);
      } else {
        participantToUpsert.setType(participantType);
      }
      // validateEvent() ensures that there is at least one organizer.
      event.processParticipantMutation(participantToUpsert, false);
      BaseDao.partialUpdate(event);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class DeleteParticipantTxn extends VoidWork {
    private final Key<Event> eventKey;
    private final Key<User> userToRemoveKey;

    public void vrun() {
      Event event = BaseDao.load(eventKey);
      if (event == null) {
        throw ErrorResponseMsg.createException("event not found",
          ErrorInfo.Type.BAD_REQUEST);
      }
      if (!event.permission.canEdit() && !userToRemoveKey.equals(getCurrentUserKey())) {
        throw ErrorResponseMsg.createException(
          "only prganizers can remove any participant from the event",
          ErrorInfo.Type.BAD_REQUEST);
      }
      EventParticipant participant = event.tryFindParticipant(userToRemoveKey);
      if (participant != null) {
        event.participants.remove(participant);
        // validateEvent() ensures that there is at least one organizer.
        event.processParticipantMutation(participant, true);
        BaseDao.partialUpdate(event);
      }
    }
  }

  @Embed
  @Data
  @NoArgsConstructor
  public static final class EventParticipant {
    @Index
    KeyWrapper<User> user;
    ParticipantType type;

    public static EventParticipant create(Key<User> user, ParticipantType type) {
      return new EventParticipant(KeyWrapper.create(user), type);
    }

    private EventParticipant(KeyWrapper<User> user, ParticipantType type) {
      this.user = user;
      this.type = type;
    }

    public static Predicate<EventParticipant> userPredicate(final Key<User> userKey) {
      return new Predicate<EventParticipant>() {
        @Override
        public boolean apply(@Nullable EventParticipant input) {
          return KeyWrapper.toKey(input.user).equals(userKey);
        }
      };
    }
  }

  public static void mutateEventReview(Key<Event> eventKey, @Nullable Review review) {
    ofy().transact(new MutateEventReviewTxn(eventKey, review));
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class MutateEventReviewTxn extends VoidWork {
    private final Key<Event> eventKey;
    @Nullable
    private final Review review;

    public void vrun() {
      Event event = BaseDao.load(eventKey);
      if (event == null) {
        throw ErrorResponseMsg.createException("event not found", ErrorInfo.Type.BAD_REQUEST);
      }
      EventParticipant participantDetails = event.tryFindParticipant(getCurrentUserKey());
      if ((participantDetails == null) ||
          (participantDetails.getType() == ParticipantType.WAIT_LISTED)) {
        throw ErrorResponseMsg.createException(
          "only users that participated in the event can provide an event review",
          ErrorInfo.Type.BAD_REQUEST);
      }
      Key<Review> expReviewKey = Review.getKey(eventKey);
      if (review != null) {
        review.preUpsertInit(eventKey);
        if (!Key.create(review).equals(expReviewKey)) {
          throw ErrorResponseMsg.createException(
            format("review key [%s] does not match expected review key [%s]",
              Key.create(review).toString(), expReviewKey.toString()),
            ErrorInfo.Type.BAD_REQUEST);
        }
      }
      Review existingReview = BaseDao.load(expReviewKey);
      if (existingReview != null) {
        event.rating.deleteRating(existingReview.getRating());
      }
      if (review == null) {
        if (existingReview != null) {
          BaseDao.delete(Key.create(existingReview));
        }
      } else {
        event.rating.addRating(review.getRating());
        BaseDao.upsert(review);
      }
      event.processRatingUpdate();
      BaseDao.partialUpdate(event);
    }
  }

  /**
   * This class updates the organizer event ratings for all organizers associated with an event.
   *
   * Note that all methods in this class should be invoked from within the context of a transaction.
   */
  @Data
  @Embed
  public static class DerivedOrganizerRatings {

    /*
     * Event has to be explicitly passed as an argument since inner classes are not supported by
     * objectify.
     */

    List<DerivedOrganizerRating> processed = Lists.newArrayList();
    List<DerivedOrganizerRating> pending = Lists.newArrayList();

    public void processParticipantMutation(Event event, EventParticipant participant,
        boolean wasDeleted) {
      processParticipantMutation(event, KeyWrapper.toKey(participant.getUser()),
        (wasDeleted ? false : (participant.getType() == ParticipantType.ORGANIZER)));
    }

    private void processParticipantMutation(Event event, Key<User> userKey, boolean isOrganizer) {
      boolean pendingQueueWasEmpty = pending.isEmpty();
      DerivedOrganizerRating derivedRating = tryFindDerivedRating(pending, userKey);
      if (derivedRating != null) {
        // Nothing to do, already queued.
        return;
      }
      derivedRating = tryFindDerivedRating(processed, userKey);
      if (derivedRating == null) {
        if (isOrganizer) {
          if (event.getRating().getCount() > 0) {
            pending.add(new DerivedOrganizerRating(userKey));
          } else {
            processed.add(new DerivedOrganizerRating(userKey));
          }
        }
      } else {
        if (isOrganizer) {
          if (!event.getRating().equals(derivedRating.getAccumulatedRating())) {
            processed.remove(derivedRating);
            pending.add(derivedRating);
          }
        } else {
          processed.remove(derivedRating);
          if (derivedRating.getAccumulatedRating().getCount() > 0) {
            pending.add(derivedRating);
          }
        }
      }
      queueProcessingTask(event, pendingQueueWasEmpty);
    }

    private static DerivedOrganizerRating tryFindDerivedRating(
        List<DerivedOrganizerRating> list, Key<User> organizerKey) {
      return Iterables.tryFind(list,
        DerivedOrganizerRating.organizerPredicate(organizerKey)).orNull();
    }

    public void processRatingUpdate(Event event) {
      boolean pendingQueueWasEmpty = pending.isEmpty();
      pending.addAll(processed);
      processed.clear();
      queueProcessingTask(event, pendingQueueWasEmpty);
    }

    private void queueProcessingTask(Event event, boolean pendingQueueWasEmpty) {
      if (pendingQueueWasEmpty && !pending.isEmpty()) {
        ProcessOrganizerRatingsServlet.enqueueTask(event);
      }
    }

    public void processPendingRating(Event event) {
      if (pending.isEmpty()) {
        return;
      }
      DerivedOrganizerRating derivedRating = pending.remove(0);
      Key<User> userKey = KeyWrapper.toKey(derivedRating.getOrganizer());
      User user = BaseDao.load(userKey);
      if (user == null) {
        // Nothing to do. User no longer exists.
        return;
      }
      // Delete the old rating.
      user.getEventOrganizerRating().deleteAggregateRating(derivedRating.getAccumulatedRating());
      // Add the new rating.
      EventParticipant eventParticipant = event.tryFindParticipant(userKey);
      if ((eventParticipant != null) && (eventParticipant.getType() == ParticipantType.ORGANIZER)) {
        derivedRating = new DerivedOrganizerRating(userKey, event.getRating());
        user.getEventOrganizerRating().addAggregateRating(derivedRating.getAccumulatedRating());
        processed.add(derivedRating);
      }
      BaseDao.partialUpdate(user);
    }

    public boolean hasPendingRatings() {
      return !pending.isEmpty();
    }

    @Data
    @Embed
    @NoArgsConstructor
    public static class DerivedOrganizerRating {
      KeyWrapper<User> organizer;
      AggregateRating accumulatedRating;

      public DerivedOrganizerRating(Key<User> organizerKey) {
        this(organizerKey, null);
      }

      public DerivedOrganizerRating(Key<User> organizerKey,
          @Nullable AggregateRating ratingToCopy) {
        this.organizer = KeyWrapper.create(organizerKey);
        accumulatedRating = AggregateRating.create(ratingToCopy);
      }

      public static Predicate<DerivedOrganizerRating> organizerPredicate(
          final Key<User> organizerKey) {
        return new Predicate<DerivedOrganizerRating>() {
          @Override
          public boolean apply(@Nullable DerivedOrganizerRating input) {
            return KeyWrapper.toKey(input.organizer).equals(organizerKey);
          }
        };
      }
    }
  }

  public static void processDerivedOrganizerRatings(Key<Event> eventKey) {
    ProcessDerivedOrganizerRatingsTxn organizerRatingsTxn;
    do {
      organizerRatingsTxn = new ProcessDerivedOrganizerRatingsTxn(eventKey);
      ofy().transact(organizerRatingsTxn);
    } while (organizerRatingsTxn.isWorkPending());
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class ProcessDerivedOrganizerRatingsTxn extends VoidWork {
    private final Key<Event> eventKey;
    private boolean workPending;

    public void vrun() {
      Event event = BaseDao.load(eventKey);
      if ((event == null) || !event.hasPendingRatings()) {
        // The event may no longer exist or there may be no work pending.
        return;
      }
      event.processPendingRating();
      workPending = event.hasPendingRatings();
      BaseDao.partialUpdate(event);
    }
  }
}
