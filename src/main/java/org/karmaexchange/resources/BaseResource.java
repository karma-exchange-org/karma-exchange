package org.karmaexchange.resources;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
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
  public T getResource(@PathParam("resource") long id) {
    T resource = ofy().load().key(Key.create(getResourceClass(), id)).get();
    return checkNotNull(resource, "Resource entity with id=" + id + " does not exist");
  }

  @Path("{resource}")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateResource(@PathParam("resource") long id, T resource) {
    checkArgument(id == getResourceId(resource),
        "The resource id of the updated entity does not match the resource id in " +
        "the resource path");
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
