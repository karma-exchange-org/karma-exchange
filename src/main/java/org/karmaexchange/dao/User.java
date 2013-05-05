package org.karmaexchange.dao;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
public final class User {

  @Id private Long id;
  private ModificationInfo modificationInfo;

  @Index
  private String firstName;
  @Index
  private String lastName;
  @Index
  private String nickName;
  private Image displayImage;
  private ContactInfo contactInfo;
  private List<EmergencyContact> emergencyContacts;

  @Index
  private List<KeyWrapper<Skill>> skills;

  // Skipping interests for now.
  // Facebook has a detailed + categorized breakdown of interests.

  @Index
  private long karmaPoints;

  private Rating eventOrganizerRating;

  private EventSearch lastEventSearch;

  // TODO(avaliani): profileSecurityPrefs
}
