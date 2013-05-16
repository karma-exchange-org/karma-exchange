package org.karmaexchange.resources.msg;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.Status;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.ParticipantImage;
import org.karmaexchange.dao.Permission;

import com.google.common.collect.Lists;

import lombok.Data;

@XmlRootElement
@Data
public class EventSearchView {

  private String key;
  private Permission permission;

  private String title;
  private Location location;
  private Date startTime;
  private Date endTime;
  private ImageUrlView primaryImage;
  private int karmaPoints;
  private Status status;

  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private int numParticipants;

  public static List<EventSearchView> create(List<Event> events) {
    List<EventSearchView> searchResults = Lists.newArrayListWithCapacity(events.size());
    for (Event event : events) {
      searchResults.add(create(event));
    }
    return searchResults;
  }

  private static EventSearchView create(Event event) {
    EventSearchView searchView = new EventSearchView();
    searchView.setKey(event.getKey());
    searchView.setPermission(event.getPermission());
    searchView.setTitle(event.getTitle());
    searchView.setLocation(event.getLocation());
    searchView.setStartTime(event.getStartTime());
    searchView.setEndTime(event.getEndTime());
    if (event.getPrimaryImage() != null) {
      searchView.setPrimaryImage(ImageUrlView.create(event.getPrimaryImage()));
    }
    searchView.setKarmaPoints(event.getKarmaPoints());
    searchView.setCachedParticipantImages(event.getCachedParticipantImages());
    searchView.setNumParticipants(event.getNumParticipants());
    searchView.setStatus(event.getStatus());
    return searchView;
  }
}
