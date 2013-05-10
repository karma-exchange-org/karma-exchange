package org.karmaexchange.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.User;

@Path("/me")
public class MeResource extends AuthenticatedResource {

  @Context
  private UriInfo uriInfo;
  @Context
  private Request request;

  public MeResource(@Context HttpServletRequest servletRequest) {
    super(servletRequest);
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource() {
    return Response.ok(getUser()).build();
  }

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(User updatedUser) {
    updatedUser.update(getUser(), getUser());
    return Response.ok().build();
  }

  @DELETE
  public void deleteResource() {
    getUser().delete();
    // TODO(avaliani): revoke OAuth credentials. This way the user account won't be re-created
    //     automatically.
  }
}
