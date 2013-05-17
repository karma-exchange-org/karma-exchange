package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.karmaexchange.dao.Event.AddRegisteredUserTxn;
import org.karmaexchange.dao.Event.DeleteRegisteredUserTxn;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.EventParticipantView;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

@Path("/event")
public class EventResource extends BaseDaoResource<Event> {

  private static final String START_TIME_PARAM = "start_time";

  @Override
  protected Class<Event> getResourceClass() {
    return Event.class;
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getResources(
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue("25") int limit,
      @QueryParam(START_TIME_PARAM) Long startTimeValue) {

    Date startTime = (startTimeValue == null) ? new Date() : new Date(startTimeValue);

    Query<Event> query = ofy().load().type(Event.class)
        .filter("startTime >=", startTime)
        .order("startTime")
        .limit(limit);
    if (afterCursorStr != null) {
      query = query.startAt(Cursor.fromWebSafeString(afterCursorStr));
    }

    QueryResultIterator<Event> queryIter = query.iterator();
    List<Event> searchResults = Lists.newArrayList(queryIter);
    BaseDao.processLoadResults(searchResults);
    Cursor afterCursor = queryIter.getCursor();

    Map<String, Object> paginationParams = Maps.newHashMap();
    paginationParams.put(START_TIME_PARAM, startTime.getTime());

    return ListResponseMsg.create(
      EventSearchView.create(searchResults),
      PagingInfo.create(afterCursor, limit, searchResults.size(), uriInfo.getAbsolutePath(),
        paginationParams));
  }

  @Path("{resource}/registered")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventParticipantView> getRegistered(
      @PathParam("resource") String eventKeyStr,
      @QueryParam("limit") @DefaultValue("10") int limit) {
    Event event = getResourceObj(eventKeyStr);
    // TODO(avaliani): consider supporting a negative limit - reverse the list.
    limit = Math.abs(limit);
    List<KeyWrapper<User>> registeredUsersBatch =
        event.getRegisteredUsers().subList(0, Math.min(limit, event.getRegisteredUsers().size()));
    return ListResponseMsg.create(EventParticipantView.get(registeredUsersBatch));
  }

  @Path("{resource}/registered")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateRegistered(@PathParam("resource") String eventKeyStr,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    ofy().transact(new AddRegisteredUserTxn(Key.<Event>create(eventKeyStr), userKey));
    return Response.ok().build();
  }

  @Path("{resource}/registered")
  @DELETE
  public void deleteRegistered(@PathParam("resource") String eventKeyStr,
      @QueryParam("user") String userKeyStr) {
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    ofy().transact(new DeleteRegisteredUserTxn(Key.<Event>create(eventKeyStr), userKey));
  }
}
