package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.UserUsage;

@Path(UserUsageResource.RESOURCE_PATH)
public class UserUsageResource {

  public static final String RESOURCE_PATH = "/admin/user_usage";

  @Context
  protected UriInfo uriInfo;

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<UserUsage> getResources() {
    return ofy().load().type(UserUsage.class).list();
  }

}
