package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.Cause;

@Path("/cause")
public class CauseResource extends BaseNamedResource<Cause> {

  @Override
  protected Class<Cause> getResourceClass() {
    return Cause.class;
  }

  @Override
  protected String getResourceName(Cause cause) {
    return cause.getId();
  }
}
