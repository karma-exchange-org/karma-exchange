package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.EventParticipantView;
import org.karmaexchange.resources.msg.ListResponseMsg;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@Path("/event")
public class EventResource extends BaseDaoResource<Event> {

  public EventResource(@Context HttpServletRequest servletRequest) {
    super(servletRequest);
  }

  @Override
  protected Class<Event> getResourceClass() {
    return Event.class;
  }

  @Path("{resource}/registered")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventParticipantView> getRegistered(@PathParam("resource") String key,
      @DefaultValue("10") @QueryParam("limit") int limit) {
    Event event = getResourceObj(key);
    ListResponseMsg<EventParticipantView> responseMsg = new ListResponseMsg<EventParticipantView>();
    List<EventParticipantView> registeredUsers = Lists.newArrayList();
    responseMsg.setData(registeredUsers);
    if (event.getRegisteredUsers() != null) {
      // TODO(avaliani): consider supporting a negative limit - reverse the list.
      limit = Math.abs(limit);
      List<KeyWrapper<User>> initialLimitRegisteredUsers =
          event.getRegisteredUsers().subList(0, Math.min(limit, event.getRegisteredUsers().size()));
      List<Key<User>> registeredUserKeys = KeyWrapper.getKeyObjs(initialLimitRegisteredUsers);
      Map<Key<User>, User> registerdUsersMap = ofy().load().keys(registeredUserKeys);
      for (User user : registerdUsersMap.values()) {
        registeredUsers.add(EventParticipantView.create(user));
      }
    }
    return responseMsg;
  }

  /*
  @Path("{resource}/registered")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateRegistered(@PathParam("resource") String key, T resource) {
    if (((resource.getKey() != null) || (resource.getId() != null))
        && !key.equals(resource.getKey())) {
      throw ErrorResponseMsg.createException(
        format("the resource key [%s] does not match the url path key [%s]",
          resource.getKey(), key),
        ErrorInfo.Type.BAD_REQUEST);
    }
    BaseDao.<T>upsert(resource, getUser());
    return Response.created(uriInfo.getAbsolutePath()).build();
  }

  @Path("{resource}/registered")
  @DELETE
  public void deleteRegistered(@PathParam("resource") String key) {
    ofy().delete().key(Key.<T>create(key)).now();
  }
*/
}
