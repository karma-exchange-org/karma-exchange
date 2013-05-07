package org.karmaexchange.dao;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
public final class Event {

  /*
   * TODO(avaliani):
   *   - look at volunteer match schema
   *   - compare this to Meetup, OneBrick, Golden Gate athletic club, etc.
   */

  @Id private Long id;
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
  private int minRsvp;
  private int maxRsvp;
  private int maxWaitingList;
  @Index
  private List<KeyWrapper<User>> rsvpdUsers;
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
}
