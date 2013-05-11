package org.karmaexchange.dao;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

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

  /*
   * TODO(avaliani):
   *   - look at volunteer match schema
   *   - compare this to Meetup, OneBrick, Golden Gate athletic club, etc.
   */

  @Id private Long id;
  @Ignore
  private String key;
  private ModificationInfo modificationInfo;

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

  @Index
  private Status status;
  private int minRegistrations;
  // The maxRegistration limit only applies to participants. The limit does not include organizers.
  private int maxRegistrations;
  private int maxWaitingList;
  @Index
  private List<KeyWrapper<User>> registeredUsers = Lists.newArrayList();
  private List<KeyWrapper<User>> waitingListUsers = Lists.newArrayList();

  private Rating eventRating;

  /**
   * The number of karma points earned by participating in the event.
   */
  @Index
  private int karmaPoints;

  public enum Status {
    OPEN,
    FULL,
    COMPLETED,
    HOLD,  // Edit only. Not visible for public search.
    CANCELED
  }

  @Override
  public void setId(Long id) {
    this.id = id;
    updateKey();
  }

  @Override
  protected void processUpdate(Event prevObj) {
    super.processUpdate(prevObj);
    processWaitingList();
  }

  private void processWaitingList() {
    if ((getRegisteredUsers().size() < getMaxRegistrations()) &&
        !getWaitingListUsers().isEmpty()) {
      int numSpots = getMaxRegistrations() - getRegisteredUsers().size();
      List<KeyWrapper<User>> usersToRegister =
          getWaitingListUsers().subList(0, Math.min(numSpots, getWaitingListUsers().size()));
      getRegisteredUsers().addAll(usersToRegister);
      usersToRegister.clear();
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
      // If the user is already part of the event or an organizer, do not add the user.
      event.getRegisteredUsers().remove(KeyWrapper.create(userKey));
      event.update(event);
    }
  }
}
