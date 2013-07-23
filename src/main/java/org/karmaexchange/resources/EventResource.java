package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.Event.UpsertParticipantTxn;
import org.karmaexchange.dao.Event.DeleteParticipantTxn;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.EventParticipantView;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ExpandedEventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;
import org.karmaexchange.resources.msg.ReviewCommentView;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;
import org.karmaexchange.util.PaginatedQuery.FilterQueryClause;
import org.karmaexchange.util.SearchUtil;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;

@Path("/event")
public class EventResource extends BaseDaoResource<Event> {

  public static final String START_TIME_PARAM = "start_time";
  public static final String SEARCH_TYPE_PARAM = "type";
  public static final String PARTICIPANT_TYPE_PARAM = "participant_type";
  public static final String KEYWORDS_PARAM = "keywords";

  public static final int DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS = 10;
  public static final int DEFAULT_NUM_REVIEWS = 3;

  public static final int MAX_SEARCH_KEYWORDS = 20;

  public enum EventSearchType {
    UPCOMING,
    PAST
  }

  @Override
  protected Class<Event> getResourceClass() {
    return Event.class;
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getResources() {
    return eventSearch(uriInfo, ImmutableList.<FilterQueryClause>of(), true);
  }

  public static ListResponseMsg<EventSearchView> eventSearch(UriInfo uriInfo,
      Collection<? extends FilterQueryClause> filters, boolean loadReviews) {
    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
    EventSearchType searchType = queryParams.containsKey(SEARCH_TYPE_PARAM) ?
        EventSearchType.valueOf(queryParams.getFirst(SEARCH_TYPE_PARAM)) : null;
    Long startTimeValue = queryParams.containsKey(START_TIME_PARAM) ?
        Long.valueOf(queryParams.getFirst(START_TIME_PARAM)) : null;
    String keywords = queryParams.getFirst(KEYWORDS_PARAM);

    PaginatedQuery.Builder<Event> queryBuilder =
        PaginatedQuery.Builder.create(Event.class, uriInfo, DEFAULT_NUM_SEARCH_RESULTS)
        .addFilters(filters);
    Date startTime = (startTimeValue == null) ? new Date() : new Date(startTimeValue);
    if (searchType == null) {
      searchType = EventSearchType.UPCOMING;
    }
    queryBuilder.setOrder((searchType == EventSearchType.UPCOMING) ? "startTime" : "-startTime");
    queryBuilder.addFilter(new ConditionFilter(
      (searchType == EventSearchType.UPCOMING) ? "startTime >=" : "startTime <", startTime));
    if (keywords != null) {
      queryBuilder.addFilter(new ConditionFilter("searchableTokens",
        SearchUtil.getSearchableTokens(keywords, MAX_SEARCH_KEYWORDS).toArray()));
    }

    PaginatedQuery.Result<Event> queryResult = queryBuilder.build().execute();
    return ListResponseMsg.create(
      EventSearchView.create(queryResult.getSearchResults(), searchType, loadReviews),
      queryResult.getPagingInfo());
  }

  @Path("{event_key}/expanded_search_view")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getExpandedEventSearchView(
    @PathParam("event_key") String eventKeyStr) {
    Event event = getResourceObj(eventKeyStr);
    return Response.ok(ExpandedEventSearchView.create(event)).build();
  }

  @Path("{event_key}/participants/{participant_type}")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
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
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertParticipants(
      @PathParam("event_key") String eventKeyStr,
      @PathParam("participant_type") ParticipantType participantType,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    ofy().transact(new UpsertParticipantTxn(
      Key.<Event>create(eventKeyStr), userKey, participantType));
    return Response.ok().build();
  }

  @Path("{event_key}/participants")
  @DELETE
  public void deleteParticipants(
      @PathParam("event_key") String eventKeyStr,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    ofy().transact(new DeleteParticipantTxn(Key.<Event>create(eventKeyStr), userKey));
  }

  @Path("{event_key}/review")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Review getReview(
      @PathParam("event_key") String eventKeyStr) {
    return BaseDao.load(Review.getKeyForCurrentUser(Key.<Event>create(eventKeyStr)));
  }

  @Path("{event_key}/review")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
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
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
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
