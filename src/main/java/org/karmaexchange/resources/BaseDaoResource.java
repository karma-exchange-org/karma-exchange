package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.net.URI;

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

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public abstract class BaseDaoResource<T extends BaseDao<T>> {

  public static final String DEFAULT_NUM_SEARCH_RESULTS = 25 + "";

  @Context
  protected UriInfo uriInfo;
  @Context
  protected Request request;

  // TODO(avaliani): re-add support for list resources. Dig into why response objects don't parse
  //     lists.
  /*
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<T> getResources() {
    return BaseDao.loadAll(getResourceClass());
  }
  */

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(T resource) {
    BaseDao.upsert(resource);
    URI uri = uriInfo.getAbsolutePathBuilder().path(resource.getKey()).build();
    return Response.created(uri).build();
  }

  @Path("{resource}")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource(@PathParam("resource") String key) {
    return Response.ok(getResourceObj(key)).build();
  }

  protected T getResourceObj(String key) {
    T resource = BaseDao.<T>load(key);
    if (resource == null) {
      throw ErrorResponseMsg.createException("resource does not exist", ErrorInfo.Type.BAD_REQUEST);
    }
    return resource;
  }

  // TODO(avaliani): This is a non CAS update. This may not be safe.
  @Path("{resource}")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateResource(@PathParam("resource") String key, T resource) {
    if (resource.isKeyComplete() && !key.equals(Key.create(resource))) {
      throw ErrorResponseMsg.createException(
        format("the resource key [%s] does not match the url path key [%s]",
          Key.create(resource), key),
        ErrorInfo.Type.BAD_REQUEST);
    }
    BaseDao.<T>upsert(resource);
    return Response.created(uriInfo.getAbsolutePath()).build();
  }

  @Path("{resource}")
  @DELETE
  public void deleteResource(@PathParam("resource") String key) {
    ofy().delete().key(Key.<T>create(key)).now();
  }

  protected abstract Class<T> getResourceClass();
}
