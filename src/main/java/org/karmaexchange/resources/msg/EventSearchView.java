package org.karmaexchange.resources.msg;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Location;

import com.google.common.collect.Lists;

import lombok.Data;

@XmlRootElement
@Data
public class EventSearchView {

  private String key;
  private String title;
  private Location location;
  private Date startTime;
  private Date endTime;
  private ImageUrlView primaryImage;
  private int karmaPoints;

  private List<ImageUrlView> participants = Lists.newArrayList();

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
    searchView.setTitle(event.getTitle());
    searchView.setLocation(event.getLocation());
    searchView.setStartTime(event.getStartTime());
    searchView.setEndTime(event.getEndTime());
    if (event.getPrimaryImage() != null) {
      searchView.setPrimaryImage(ImageUrlView.create(event.getPrimaryImage()));
    }
    searchView.setKarmaPoints(event.getKarmaPoints());
    // searchView.setParticipants();
    return searchView;
  }
}
