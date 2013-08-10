package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.AlbumRef;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.RegistrationInfo;
import org.karmaexchange.dao.Event.Status;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.OrganizationNamedKeyWrapper;
import org.karmaexchange.dao.ParticipantImage;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.Review;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.resources.msg.EventView.OrgDetails;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public class EventSearchView {

  private String key;
  private Permission permission;

  private KeyWrapper<Organization> organization;
  private OrgDetails organizationDetails;
  private List<OrganizationNamedKeyWrapper> associatedOrganizations = Lists.newArrayList();

  private String title;
  private Location location;
  private Date startTime;
  private Date endTime;
  private Status status;
  private AlbumRef album;
  private RegistrationInfo registrationInfo;

  private List<ParticipantImage> cachedParticipantImages = Lists.newArrayList();

  private int numAttending;
  private int numRegistered;
  private int maxRegistrations;

  private AggregateRating rating;
  private Rating currentUserRating;
  private int karmaPoints;

  public static List<EventSearchView> create(List<Event> events, EventSearchType searchType,
      boolean loadReviews) {
    // Do this first since the load of reviews is currently blocking.
    Map<Key<Organization>, Organization> orgs = unprocessedLoadOrgs(events);

    Map<Key<Review>, Review> reviews;
    if (loadReviews && (searchType == EventSearchType.PAST)) {
      reviews = loadEventReviews(events);
    } else {
      reviews = Maps.newHashMap();
    }

    // Finish the processing. This is a bit hacky... ideally BaseDao would provide an
    // asynchronous wrapper that would process the object on first access.
    BaseDao.processLoadResults(orgs.values());

    List<EventSearchView> searchResults = Lists.newArrayList();
    for (Event event : events) {
      searchResults.add(
        new EventSearchView(event, orgs.get(KeyWrapper.toKey(event.getOrganization())),
          reviews.get(Review.getKeyForCurrentUser(event))));
    }
    return searchResults;
  }

  private static Map<Key<Organization>, Organization> unprocessedLoadOrgs(List<Event> events) {
    Set<Key<Organization>> orgs = Sets.newHashSet();
    for (Event event : events) {
      orgs.add(KeyWrapper.toKey(event.getOrganization()));
    }
    return ofy().load().keys(orgs);
  }

  private static Map<Key<Review>, Review> loadEventReviews(List<Event> events) {
    List<Key<Review>> reviewKeys = Lists.newArrayListWithCapacity(events.size());
    for (Event event : events) {
      // Only fetch the review if the current user is registered for the event.
      if (event.getRegistrationInfo() == RegistrationInfo.REGISTERED) {
        reviewKeys.add(Review.getKeyForCurrentUser(event));
      }
    }
    Map<Key<Review>, Review> reviews = ofy().load().keys(reviewKeys);
    BaseDao.processLoadResults(reviews.values());
    return reviews;
  }

  protected EventSearchView(Event event, @Nullable Organization fetchedOrg,
      @Nullable Review currentUserReview) {
    key = event.getKey();
    permission = event.getPermission();

    organization = event.getOrganization();
    if (fetchedOrg != null) {
      organizationDetails = new OrgDetails(fetchedOrg);
    }
    associatedOrganizations = event.getAssociatedOrganizations();

    title = event.getTitle();
    location = event.getLocation();
    startTime = event.getStartTime();
    endTime = event.getEndTime();
    status = event.getStatus();
    album = event.getAlbum();
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
