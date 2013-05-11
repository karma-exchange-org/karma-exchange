package org.karmaexchange.dao;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
public class EventComment {

  @Id private Long id;

  @Index
  private KeyWrapper<Event> event;
  private String comment;
  @Index
  private Date creationDate;
  @Index
  private Type type;
  private KeyWrapper<User> user;
  /**
   * The rating specified for event / organizer rating comments. Saving the rating provides
   * the ability to update the rating if required.
   */
  private Double rating;

  /**
   * Users tagged identifies the set of users the comment was targeted for. For an
   * ORAGANIZER_RATING comment only the organizer will be tagged.
   */
  @Index
  private List<KeyWrapper<User>> usersTagged = Lists.newArrayList();

  public enum Type {
    LOGISTIC,
    EVENT_RATING,
    ORGANIZER_RATING,
    IMPACT
  }
}
