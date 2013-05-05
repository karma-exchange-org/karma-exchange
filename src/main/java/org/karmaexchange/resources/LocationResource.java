package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.Location;

@Path("/location")
public class LocationResource extends BaseResource<Location> {

  @Override
  protected Class<Location> getResourceClass() {
    return Location.class;
  }

  @Override
  protected long getResourceId(Location location) {
    return location.getId();
  }
}
