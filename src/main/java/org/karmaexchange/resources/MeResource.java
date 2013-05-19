package org.karmaexchange.resources;

import static org.karmaexchange.resources.BaseDaoResource.DEFAULT_NUM_SEARCH_RESULTS;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUser;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.User;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;

import com.google.common.collect.Maps;

@Path("/me")
public class MeResource {

  @Context
  private UriInfo uriInfo;
  @Context
  private Request request;

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource() {
    return Response.ok(getCurrentUser()).build();
  }

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(User updatedUser) {
    updatedUser.update(getCurrentUser());
    return Response.ok().build();
  }

  @DELETE
  public void deleteResource() {
    ofy().delete().key(getCurrentUserKey()).now();
    // TODO(avaliani): revoke OAuth credentials. This way the user account won't be re-created
    //     automatically.
  }

  @Path("event")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getEvents(
      @QueryParam(EventResource.SEARCH_TYPE_PARAM) EventSearchType searchType,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_SEARCH_RESULTS) int limit,
      @QueryParam(EventResource.START_TIME_PARAM) Long startTimeValue) {
    Map<String, Object> filters = Maps.newHashMap();
    filters.put("participants.user.key", getCurrentUserKey());
    return EventResource.eventSearch(afterCursorStr, limit, startTimeValue,
      uriInfo.getAbsolutePath(), searchType, filters);
  }
}
