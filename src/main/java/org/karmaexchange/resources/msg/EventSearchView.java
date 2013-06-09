package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.RegistrationInfo;
import org.karmaexchange.dao.Event.Status;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.ParticipantImage;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.Review;
import org.karmaexchange.resources.EventResource.EventSearchType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

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
  private Status status;
  private ImageUrlView primaryImage;
  private RegistrationInfo registrationInfo;

  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private int numAttending;
  private int numRegistered;
  private int maxRegistrations;

  private AggregateRating rating;
  private Rating currentUserRating;
  private int karmaPoints;

  public static List<EventSearchView> create(List<Event> events, EventSearchType searchType) {
    Map<Key<Review>, Review> reviews;
    if (searchType == EventSearchType.PAST) {
      reviews = loadEventReviews(events);
    } else {
      reviews = Maps.newHashMap();
    }
    List<EventSearchView> searchResults = Lists.newArrayListWithCapacity(events.size());
    for (Event event : events) {
      searchResults.add(new EventSearchView(event, reviews.get(Review.getKey(event))));
    }
    return searchResults;
  }

  private static Map<Key<Review>, Review> loadEventReviews(List<Event> events) {
    List<Key<Review>> reviewKeys = Lists.newArrayListWithCapacity(events.size());
    for (Event event : events) {
      reviewKeys.add(Review.getKey(event));
    }
    Map<Key<Review>, Review> reviews = ofy().load().keys(reviewKeys);
    BaseDao.processLoadResults(reviews.values());
    return reviews;
  }

  protected EventSearchView(Event event, @Nullable Review currentUserReview) {
    key = event.getKey();
    permission = event.getPermission();
    title = event.getTitle();
    location = event.getLocation();
    startTime = event.getStartTime();
    endTime = event.getEndTime();
    status = event.getStatus();
    if (event.getPrimaryImage() != null) {
      // PrimaryImage(ImageUrlView.create(event.getPrimaryImage()));
    }
    karmaPoints = event.getKarmaPoints();
    cachedParticipantImages = event.getCachedParticipantImages();
    numAttending = event.getNumAttending();
    numRegistered = event.getRegisteredUsers().size();
    maxRegistrations = event.getMaxRegistrations();
    registrationInfo = event.getRegistrationInfo();
    rating = event.getRating();
    if (currentUserReview != null) {
      currentUserRating = currentUserReview.getRating();
    }
  }
}
