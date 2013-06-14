package org.karmaexchange.resources;

import static com.google.common.base.Preconditions.checkState;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.net.URI;
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
import javax.ws.rs.core.Response;

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
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.FilterQueryClause;
import org.karmaexchange.util.PaginatedQuery.OrderQueryClause;
import org.karmaexchange.util.PaginationParam;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

@Path("/event")
public class EventResource extends BaseDaoResource<Event> {

  public static final String START_TIME_PARAM = "start_time";
  public static final String SEARCH_TYPE_PARAM = "type";
  public static final String PARTICIPANT_TYPE_PARAM = "participant_type";

  public static final String DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS = 10 + "";
  public static final String DEFAULT_NUM_REVIEWS = 3 + "";

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
  public ListResponseMsg<EventSearchView> getResources(
      @QueryParam(SEARCH_TYPE_PARAM) EventSearchType searchType,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_SEARCH_RESULTS) int limit,
      @QueryParam(START_TIME_PARAM) Long startTimeValue) {
    return eventSearch(afterCursorStr, limit, startTimeValue, uriInfo.getAbsolutePath(),
      searchType, ImmutableList.<FilterQueryClause>of(), true);
  }

  public static ListResponseMsg<EventSearchView> eventSearch(String afterCursorStr, int limit,
      Long startTimeValue, URI baseUri, EventSearchType searchType,
      Collection<? extends FilterQueryClause> filters, boolean loadReviews) {
    PaginatedQuery.Builder<Event> queryBuilder = PaginatedQuery.Builder.create(Event.class)
        .addFilters(filters);
    if (afterCursorStr != null) {
      queryBuilder.setAfterCursor(Cursor.fromWebSafeString(afterCursorStr));
    }
    Date startTime = (startTimeValue == null) ? new Date() : new Date(startTimeValue);
    if (searchType == null) {
      searchType = EventSearchType.UPCOMING;
    }
    queryBuilder.setOrder(new StartTimeOrder(searchType));
    queryBuilder.addFilter(new StartTimeFilter(searchType, startTime));
    // Query one more than the limit to see if we need to provide a link to additional results.
    queryBuilder.setLimit(limit + 1);

    PaginatedQuery<Event> paginatedQuery = queryBuilder.build();
    Query<Event> ofyQuery = paginatedQuery.getOfyQuery();

    QueryResultIterator<Event> queryIter = ofyQuery.iterator();
    List<Event> searchResults = Lists.newArrayList(Iterators.limit(queryIter, limit));
    Cursor afterCursor = queryIter.getCursor();
    BaseDao.processLoadResults(searchResults);

    return ListResponseMsg.create(
      EventSearchView.create(searchResults, searchType, loadReviews),
      PagingInfo.create(afterCursor, limit, queryIter.hasNext(), baseUri,
        paginatedQuery.getPaginationParams()));
  }

  private static class StartTimeFilter extends FilterQueryClause {
    public StartTimeFilter(EventSearchType searchType, Date startTime) {
      super((searchType == EventSearchType.UPCOMING) ? "startTime >=" : "startTime <",
          startTime);
      paginationParam = new PaginationParam(START_TIME_PARAM, String.valueOf(startTime.getTime()));
    }
  }

  private static class StartTimeOrder extends OrderQueryClause {
    public StartTimeOrder(EventSearchType searchType) {
      super((searchType == EventSearchType.UPCOMING) ? "startTime" : "-startTime");
      paginationParam = new PaginationParam(SEARCH_TYPE_PARAM, searchType.toString());
    }
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
      @PathParam("participant_type") ParticipantType participantType,
      @QueryParam(PagingInfo.OFFSET_PARAM) @DefaultValue("0") int offset,
      @QueryParam(PagingInfo.LIMIT_PARAM)
      @DefaultValue(DEFAULT_NUM_PARTICIPANT_VIEW_RESULTS) int limit) {
    Event event = getResourceObj(eventKeyStr);
    checkState((limit >= 0) && (offset >= 0), "limit and offset must be non-negative");
    List<KeyWrapper<User>> participants = event.getParticipants(participantType);
    List<KeyWrapper<User>> offsettedResult =
        PagingInfo.createOffsettedResult(participants, offset, limit);
    return ListResponseMsg.create(
      EventParticipantView.get(offsettedResult),
      PagingInfo.create(offset, limit, participants.size(), uriInfo.getAbsolutePath(), null));
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
    Event.mutateEventReview(Key.<Event>create(eventKeyStr), review);
  }

  @Path("{event_key}/review")
  @DELETE
  public void deleteReview(
      @PathParam("event_key") String eventKeyStr) {
    Event.mutateEventReview(Key.<Event>create(eventKeyStr), null);
  }

  @Path("{event_key}/review_comment_view")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<ReviewCommentView> getReviewComments(
      @PathParam("event_key") String eventKeyStr,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_REVIEWS) int limit) {
    String resultOrder = "-commentCreationDate";
    Key<Event> eventKey = Key.<Event>create(eventKeyStr);
    // Query one more than the limit to see if we need to provide a link to additional results.
    Query<Review> query = ofy().load().type(Review.class)
        .ancestor(eventKey)
        .order(resultOrder)
        .limit(limit + 1);
    if (afterCursorStr != null) {
      query = query.startAt(Cursor.fromWebSafeString(afterCursorStr));
    }

    QueryResultIterator<Review> queryIter = query.iterator();
    List<Review> searchResults = Lists.newArrayList(Iterators.limit(queryIter, limit));
    Cursor afterCursor = queryIter.getCursor();
    BaseDao.processLoadResults(searchResults);

    return ListResponseMsg.create(
      ReviewCommentView.create(searchResults),
      PagingInfo.create(afterCursor, limit, queryIter.hasNext(), uriInfo.getAbsolutePath(),
        Maps.<String, Object>newHashMap()));
  }
}
