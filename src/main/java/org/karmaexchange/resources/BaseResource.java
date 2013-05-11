package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public abstract class BaseResource<T> {

  @Context
  protected UriInfo uriInfo;
  @Context
  protected Request request;

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<T> getResources() {
    return ofy().load().type(getResourceClass()).list();
  }

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(T resource) {
    ofy().save().entity(resource).now();
    URI uri = uriInfo.getAbsolutePathBuilder().path(Long.toString(getResourceId(resource))).build();
    return Response.created(uri).build();
  }

  @Path("{resource}")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource(@PathParam("resource") long id) {
    T resource = ofy().load().key(Key.create(getResourceClass(), id)).now();
    if (resource == null) {
      throw ErrorResponseMsg.createException("resource does not exist", ErrorInfo.Type.BAD_REQUEST);
    }
    return Response.ok(resource).build();
  }

  @Path("{resource}")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateResource(@PathParam("resource") long id, T resource) {
    if (id != getResourceId(resource)) {
      throw ErrorResponseMsg.createException(
        format("new resource id [%s] does not match existing resource id [%s]",
          Long.toString(getResourceId(resource)), Long.toString(id)),
        ErrorInfo.Type.BAD_REQUEST);
    }
    ofy().save().entity(resource).now();
    return Response.created(uriInfo.getAbsolutePath()).build();
  }

  @Path("{resource}")
  @DELETE
  public void deleteResource(@PathParam("resource") long id) {
    ofy().delete().key(Key.create(getResourceClass(), id)).now();
  }

  protected abstract Class<T> getResourceClass();
  protected abstract long getResourceId(T resource);
}
