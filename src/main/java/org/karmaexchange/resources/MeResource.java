package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUser;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
  public Response getEvents(@QueryParam("type") String eventType) {
    // TODO(avaliani): add support for impact view type
    return Response.ok(getCurrentUser()).build();
  }
}
