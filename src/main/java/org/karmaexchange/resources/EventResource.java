package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;

import org.karmaexchange.dao.Event;

import com.googlecode.objectify.cmd.LoadType;

@Path("/event")
public class EventResource extends BaseResource<Event> {

  @Override
  public List<Event> getResources() {
    // MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
    // LoadType<Event> query = ofy().load().type(Event.class);
    // filter
    // filter={x>=y;a<b}

    return ofy().load().type(Event.class).list();
  }

  @Override
  protected Class<Event> getResourceClass() {
    return Event.class;
  }

  @Override
  protected long getResourceId(Event event) {
    return event.getId();
  }
}
