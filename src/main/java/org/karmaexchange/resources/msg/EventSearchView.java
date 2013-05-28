package org.karmaexchange.resources.msg;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.RegistrationInfo;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.ParticipantImage;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.dao.Rating;

import com.google.common.collect.Lists;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public class EventSearchView {

  private String key;
  private Permission permission;

  private String title;
  private Location location;
  private Date startTime;
  private Date endTime;
  private ImageUrlView primaryImage;
  private RegistrationInfo registrationInfo;

  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private int numAttending;
  private int numRegistered;
  private int maxRegistrations;

  private Rating eventRating;
  private int karmaPoints;

  public static List<EventSearchView> create(List<Event> events) {
    List<EventSearchView> searchResults = Lists.newArrayListWithCapacity(events.size());
    for (Event event : events) {
      searchResults.add(new EventSearchView(event));
    }
    return searchResults;
  }

  protected EventSearchView(Event event) {
    key = event.getKey();
    permission = event.getPermission();
    title = event.getTitle();
    location = event.getLocation();
    startTime = event.getStartTime();
    endTime = event.getEndTime();
    if (event.getPrimaryImage() != null) {
      // PrimaryImage(ImageUrlView.create(event.getPrimaryImage()));
    }
    karmaPoints = event.getKarmaPoints();
    cachedParticipantImages = event.getCachedParticipantImages();
    numAttending = event.getNumAttending();
    numRegistered = event.getRegisteredUsers().size();
    maxRegistrations = event.getMaxRegistrations();
    registrationInfo = event.getRegistrationInfo();
    eventRating = event.getEventRating();
  }
}
