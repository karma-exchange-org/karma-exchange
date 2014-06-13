package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.AlbumRef;
import org.karmaexchange.dao.AssociatedOrganization;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.RegistrationInfo;
import org.karmaexchange.dao.Event.Status;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.CachedEventParticipant;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.EventResource.EventSearchType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public class EventSearchView {

  private String key;
  private Permission permission;

  private KeyWrapper<Organization> organization;
  private OrgEventSummary organizationDetails;
  private List<AssociatedOrganization> associatedOrganizations = Lists.newArrayList();

  private String title;
  private Location location;
  private Date startTime;
  private Date endTime;
  private Status status;
  private AlbumRef album;
  private RegistrationInfo registrationInfo;
  private String externalRegistrationUrl;
  private String externalRegistrationDetailsHtml;
  private UserEventSearchInfo userEventSearchInfo;

  private List<CachedEventParticipant> cachedParticipants = Lists.newArrayList();

  private int numAttending;
  private int numRegistered;
  private int maxRegistrations;

  private AggregateRating rating;
  private Rating currentUserRating;
  private int karmaPoints;
  private String impactSummary;

  public static List<EventSearchView> create(List<Event> events, EventSearchType searchType,
      @Nullable Key<User> eventSearchUserKey, boolean loadReviews) {
    Map<Key<Organization>, Organization> orgs = loadOrgs(events);

    Map<Key<Review>, Review> reviews = ImmutableMap.of();
    if (loadReviews && (searchType == EventSearchType.PAST)) {
      reviews = loadEventReviews(events);
    }

    List<EventSearchView> searchResults = Lists.newArrayList();
    for (Event event : events) {
      searchResults.add(
        new EventSearchView(event, orgs.get(KeyWrapper.toKey(event.getOrganization())),
          reviews.get(Review.getKeyForCurrentUser(event)), eventSearchUserKey));
    }
    return searchResults;
  }

  private static Map<Key<Organization>, Organization> loadOrgs(List<Event> events) {
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
    return ofy().load().keys(reviewKeys);
  }

  protected EventSearchView(Event event, @Nullable Organization fetchedOrg,
      @Nullable Review currentUserReview, @Nullable Key<User> eventSearchUserKey) {
    key = event.getKey();
    permission = event.getPermission();

    organization = event.getOrganization();
    if (fetchedOrg != null) {
      organizationDetails = new OrgEventSummary(fetchedOrg);
    }
    associatedOrganizations = event.getAssociatedOrganizations();

    title = event.getTitle();
    location = event.getLocation();
    startTime = event.getStartTime();
    endTime = event.getEndTime();
    status = event.getStatus();
    album = event.getAlbum();
    karmaPoints = event.getKarmaPoints();
    impactSummary = event.getImpactSummary();
    cachedParticipants = event.getCachedParticipants();
    numAttending = event.getNumAttending();
    numRegistered = event.getRegisteredUsers().size();
    maxRegistrations = event.getMaxRegistrations();
    registrationInfo = event.getRegistrationInfo();
    externalRegistrationUrl = event.getExternalRegistrationUrl();
    externalRegistrationDetailsHtml = event.getExternalRegistrationDetailsHtml();
    rating = event.getRating();
    if (currentUserReview != null) {
      currentUserRating = currentUserReview.getRating();
    }

    if (eventSearchUserKey != null) {
      userEventSearchInfo = new UserEventSearchInfo(
        event.getRegistrationInfo(eventSearchUserKey));
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserEventSearchInfo {
    private RegistrationInfo registrationInfo;
  }
}
