package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.BaseEvent;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.EventParticipantView;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.EventView;
import org.karmaexchange.resources.msg.ExpandedEventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;
import org.karmaexchange.resources.msg.ReviewCommentView;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;
import org.karmaexchange.util.PaginatedQuery.FilterQueryClause;
import org.karmaexchange.util.SearchUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Path("/event")
@NoArgsConstructor
public class EventResource extends BaseDaoResourceEx<Event, EventView> {

  public static final String START_TIME_PARAM = "start_time";
  public static final String END_TIME_PARAM = "end_time";
  public static final String SEARCH_TYPE_PARAM = "type";
  public static final String KEYWORDS_PARAM = "keywords";

  public static final String SEARCH_DIST_PARAM = "distance";
  // Limiting the scope for distance searches allows for future geo-hashing based search
  // implementations.
  public static final List<Integer> PERMITTED_DIST_VALUES = Lists.newArrayList(5, 10 , 50);
  public static final String SEARCH_LATITUDE_PARAM = "lat";
  public static final String SEARCH_LONGITUDE_PARAM = "lng";

  public static final int DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS = 10;
  public static final int DEFAULT_NUM_REVIEWS = 3;

  public static final int MAX_SEARCH_KEYWORDS = 20;

  public enum EventSearchType {
    UPCOMING,
    PAST,
    INTERVAL
  }

  public EventResource(UriInfo uriInfo, Request request, ServletContext servletContext) {
    super(uriInfo, request, servletContext);
  }

  @Override
  protected EventView createBaseDaoView(Event event) {
    return new EventView(event);
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getResources() {
    return eventSearch(uriInfo, ImmutableList.<FilterQueryClause>of(), null);
  }

  public static ListResponseMsg<EventSearchView> eventSearch(UriInfo uriInfo,
      Collection<? extends FilterQueryClause> filters, @Nullable Key<User> eventSearchUserKey) {
    EventSearchResults eventSearchResult =
        eventSearch(uriInfo, null, filters, eventSearchUserKey);
    return ListResponseMsg.create(
      eventSearchResult.results,
      eventSearchResult.pagingInfo);
  }

  public static void eventSearchWarmup() {
    MultivaluedMap<String, String> reqParams = new MultivaluedMapImpl();
    eventSearch(null, reqParams, ImmutableList.<FilterQueryClause>of(), null);
  }

  private static EventSearchType getEventSearchType(
      MultivaluedMap<String, String> reqParams) {
    EventSearchType searchType = reqParams.containsKey(SEARCH_TYPE_PARAM) ?
        EventSearchType.valueOf(reqParams.getFirst(SEARCH_TYPE_PARAM)) : null;
    Long startTimeValue = reqParams.containsKey(START_TIME_PARAM) ?
        Long.valueOf(reqParams.getFirst(START_TIME_PARAM)) : null;
    Long endTimeValue = reqParams.containsKey(END_TIME_PARAM) ?
        Long.valueOf(reqParams.getFirst(END_TIME_PARAM)) : null;

    if (searchType == null) {
      if ((startTimeValue != null) && (endTimeValue != null)) {
        searchType = EventSearchType.INTERVAL;
      } else {
        searchType = EventSearchType.UPCOMING;
      }
    }
    return searchType;
  }

  public static<T extends BaseEvent<T>> PaginatedQuery.Result<T> eventBaseQuery(
      Class<T> eventBaseClass,
      @Nullable UriInfo uriInfo,
      @Nullable MultivaluedMap<String, String> reqParams,
      Collection<? extends FilterQueryClause> filters,
      Key<?> ancestorKey) {
    if (uriInfo != null) {
      reqParams = uriInfo.getQueryParameters();
    }
    EventSearchType searchType = getEventSearchType(reqParams);
    Long startTimeValue = reqParams.containsKey(START_TIME_PARAM) ?
        Long.valueOf(reqParams.getFirst(START_TIME_PARAM)) : null;
    Long endTimeValue = reqParams.containsKey(END_TIME_PARAM) ?
        Long.valueOf(reqParams.getFirst(END_TIME_PARAM)) : null;

    if ((endTimeValue != null) && (searchType != EventSearchType.INTERVAL))  {
      throw ErrorResponseMsg.createException(
        "parameter '" + END_TIME_PARAM + "' can only be specified " +
            "with a query '" + SEARCH_TYPE_PARAM + "' of '" + EventSearchType.INTERVAL + "'",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if ((searchType == EventSearchType.INTERVAL) &&
        ( (startTimeValue == null) || (endTimeValue == null)) ) {
      throw ErrorResponseMsg.createException(
        "parameters '" + START_TIME_PARAM + "' and '" + END_TIME_PARAM + "' must be " +
            "specified for a query '" + SEARCH_TYPE_PARAM +
            "' of '" + EventSearchType.INTERVAL + "'",
        ErrorInfo.Type.BAD_REQUEST);
    }

    Date startTime = (startTimeValue == null) ? new Date() : new Date(startTimeValue);
    Date endTime = (endTimeValue == null) ? new Date() : new Date(endTimeValue);

    PaginatedQuery.Builder<T> queryBuilder =
        PaginatedQuery.Builder.create(
            eventBaseClass, uriInfo, reqParams, DEFAULT_NUM_SEARCH_RESULTS)
        .addFilters(filters);
    queryBuilder.setOrder(
      ((searchType == EventSearchType.UPCOMING) || (searchType == EventSearchType.INTERVAL)) ?
          "startTime" : "-startTime");
    if (searchType == EventSearchType.INTERVAL) {
      queryBuilder.addFilter(new ConditionFilter("startTime >=", startTime));
      queryBuilder.addFilter(new ConditionFilter("startTime <", endTime));
    } else {
      queryBuilder.addFilter(new ConditionFilter(
        (searchType == EventSearchType.UPCOMING) ? "startTime >=" : "startTime <", startTime));
    }
    if (ancestorKey != null) {
      queryBuilder.setAncestor(ancestorKey);
    }

    return queryBuilder.build().execute();
  }

  private static EventSearchResults eventSearch(@Nullable UriInfo uriInfo,
      @Nullable MultivaluedMap<String, String> reqParams,
      Collection<? extends FilterQueryClause> filters,
      @Nullable Key<User> eventSearchUserKey) {
    if (uriInfo != null) {
      reqParams = uriInfo.getQueryParameters();
    }

    List<FilterQueryClause> combinedFilters = Lists.newArrayList(filters);
    String keywords = reqParams.getFirst(KEYWORDS_PARAM);
    if (keywords != null) {
      combinedFilters.add(new ConditionFilter("searchableTokens",
        SearchUtil.getSearchableTokens(keywords, MAX_SEARCH_KEYWORDS).toArray()));
    }

    PaginatedQuery.Result<Event> queryResult = eventBaseQuery(
      Event.class, uriInfo, reqParams, combinedFilters, null);

    PostFilteredEventSearchResults postFilteredResults =
        postFilterByDistance(reqParams, queryResult);

    boolean loadReviews = (eventSearchUserKey != null) &&
        eventSearchUserKey.equals(getCurrentUserKey());
    EventSearchType searchType = getEventSearchType(reqParams);

    return new EventSearchResults(
      EventSearchView.create(postFilteredResults.results, searchType, eventSearchUserKey,
        loadReviews),
      postFilteredResults.pagingInfo);
  }

  @Data
  private static class EventSearchResults {
    private final List<EventSearchView> results;
    @Nullable
    private final PagingInfo pagingInfo;
  }

  @Data
  private static class PostFilteredEventSearchResults {
    private final List<Event> results;
    @Nullable
    private final PagingInfo pagingInfo;
  }

  /*
   * This is not scalable at all. But, it works for demo purposes.
   * TODO(avaliani): make spatial search scalable.
   */
  private static PostFilteredEventSearchResults postFilterByDistance(
      MultivaluedMap<String, String> reqParams, PaginatedQuery.Result<Event> queryResult) {
    Integer maxDistInMiles = reqParams.containsKey(SEARCH_DIST_PARAM) ?
        Integer.valueOf(reqParams.getFirst(SEARCH_DIST_PARAM)) : null;
    Double lattitude = reqParams.containsKey(SEARCH_LATITUDE_PARAM) ?
        Double.valueOf(reqParams.getFirst(SEARCH_LATITUDE_PARAM)) : null;
    Double longitude = reqParams.containsKey(SEARCH_LONGITUDE_PARAM) ?
        Double.valueOf(reqParams.getFirst(SEARCH_LONGITUDE_PARAM)) : null;
    if (maxDistInMiles == null) {
      return new PostFilteredEventSearchResults(queryResult.getSearchResults(),
        queryResult.getPagingInfo());
    }
    if (!PERMITTED_DIST_VALUES.contains(maxDistInMiles)) {
      throw ErrorResponseMsg.createException(
        "invalid value for parameter '" + SEARCH_DIST_PARAM + "', permitted values: " +
            PERMITTED_DIST_VALUES,
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (lattitude == null) {
      throw ErrorResponseMsg.createException("parameter not specified: " + SEARCH_LATITUDE_PARAM,
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (longitude == null) {
      throw ErrorResponseMsg.createException("parameter not specified: " + SEARCH_LONGITUDE_PARAM,
        ErrorInfo.Type.BAD_REQUEST);
    }
    LatLng userLoc = new LatLng(lattitude, longitude);

    List<Event> filteredResults = Lists.newArrayList();
    do {
      for (Event event : queryResult.getSearchResults()) {
        GeoPtWrapper eventGeoPt = event.getLocation().getAddress().getGeoPt();
        LatLng eventLoc = new LatLng(eventGeoPt.getLatitude(), eventGeoPt.getLongitude());
        double distanceInMiles = LatLngTool.distance(userLoc, eventLoc, LengthUnit.MILE);
        if (distanceInMiles <= maxDistInMiles) {
          filteredResults.add(event);
        }
      }

      if ((filteredResults.size() < queryResult.getQuery().getLimit()) &&
          queryResult.hasMoreResults()) {
        queryResult = queryResult.fetchNextBatch();
      } else {
        break;
      }
    } while (true);

    return new PostFilteredEventSearchResults(filteredResults, queryResult.getPagingInfo());
  }

  @Path("{event_key}/expanded_search_view")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public Response getExpandedEventSearchView(
    @PathParam("event_key") String eventKeyStr) {
    Event event = getResourceObj(eventKeyStr);
    return Response.ok(ExpandedEventSearchView.create(event)).build();
  }

  @Path("{event_key}/participants/{participant_type}")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventParticipantView> getParticipants(
      @PathParam("event_key") String eventKeyStr,
      @PathParam("participant_type") ParticipantType participantType) {
    Event event = getResourceObj(eventKeyStr);
    List<KeyWrapper<User>> participants = event.getParticipants(participantType);
    List<KeyWrapper<User>> offsettedResult =
        PagingInfo.offsetResult(participants, uriInfo, DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS);
    return ListResponseMsg.create(
      EventParticipantView.get(offsettedResult),
      PagingInfo.create(participants.size(), uriInfo, DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS));
  }

  @Path("{event_key}/participants/{participant_type}")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  public Response upsertParticipants(
      @PathParam("event_key") String eventKeyStr,
      @PathParam("participant_type") ParticipantType participantType,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey =
        (userKeyStr == null) ? getCurrentUserKey() : OfyUtil.<User>createKey(userKeyStr);
    Key<Event> eventKey =  OfyUtil.createKey(eventKeyStr);
    Event.upsertParticipant(eventKey, userKey, participantType);
    return Response.ok().build();
  }

  @Path("{event_key}/participants")
  @DELETE
  public void deleteParticipants(
      @PathParam("event_key") String eventKeyStr,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    Key<Event> eventKey =  OfyUtil.createKey(eventKeyStr);
    Event.deleteParticipant(eventKey, userKey);
  }

  @Path("{event_key}/review")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public Review getReview(
      @PathParam("event_key") String eventKeyStr) {
    return ofy().load().key(Review.getKeyForCurrentUser(Key.<Event>create(eventKeyStr))).now();
  }

  @Path("{event_key}/review")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  public void upsertReview(
      @PathParam("event_key") String eventKeyStr,
      Review review) {
    Event.mutateEventReviewForCurrentUser(Key.<Event>create(eventKeyStr), review);
  }

  @Path("{event_key}/review")
  @DELETE
  public void deleteReview(
      @PathParam("event_key") String eventKeyStr) {
    Event.mutateEventReviewForCurrentUser(Key.<Event>create(eventKeyStr), null);
  }

  @Path("{event_key}/review_comment_view")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public ListResponseMsg<ReviewCommentView> getReviewComments(
      @PathParam("event_key") String eventKeyStr) {
    Key<Event> eventKey = OfyUtil.createKey(eventKeyStr);
    PaginatedQuery.Builder<Review> queryBuilder =
        PaginatedQuery.Builder.create(Review.class, uriInfo, DEFAULT_NUM_REVIEWS)
        .setAncestor(eventKey)
        .setOrder("-commentCreationDate");
    PaginatedQuery.Result<Review> queryResult = queryBuilder.build().execute();
    return ListResponseMsg.create(
      ReviewCommentView.create(queryResult.getSearchResults()),
      queryResult.getPagingInfo());
  }
}
