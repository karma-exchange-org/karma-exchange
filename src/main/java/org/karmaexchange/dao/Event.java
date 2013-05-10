package org.karmaexchange.dao;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
  private List<KeyWrapper<Cause>> causes;

  private Location location;
  @Index
  private Date startTime;
  @Index
  private Date endTime;

  private Image primaryImage;
  private List<Image> allImages;

  @Index
  private List<KeyWrapper<Skill>> skillsPreferred;
  @Index
  private List<KeyWrapper<Skill>> skillsRequired;

  // Organizations can co-host events. Admins of the organization can edit the event.
  @Index
  private List<KeyWrapper<Organization>> organizations;
  // Organizers can also edit the event.
  @Index
  private List<KeyWrapper<User>> organizers;

  @Index
  private Status status;
  private int minRegistrations;
  private int maxRegistrations;
  private int maxWaitingList;
  @Index
  private List<KeyWrapper<User>> registeredUsers;
  private List<KeyWrapper<User>> waitingListUsers;

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
}
