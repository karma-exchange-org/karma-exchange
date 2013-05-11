package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

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
import org.karmaexchange.dao.Event.AddRegisteredUserTxn;
import org.karmaexchange.dao.Event.DeleteRegisteredUserTxn;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.EventParticipantView;
import org.karmaexchange.resources.msg.ListResponseMsg;

import com.googlecode.objectify.Key;

@Path("/event")
public class EventResource extends BaseDaoResource<Event> {

  @Override
  protected Class<Event> getResourceClass() {
    return Event.class;
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
