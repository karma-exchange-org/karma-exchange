package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.net.URI;

import javax.servlet.ServletContext;
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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.resources.msg.BaseDaoView;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.OfyUtil;

import com.googlecode.objectify.Key;

@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseDaoResourceEx<T extends BaseDao<T>, U extends BaseDaoView<T>> {

  public static final int DEFAULT_NUM_SEARCH_RESULTS = 25;

  @Context
  protected UriInfo uriInfo;
  @Context
  protected Request request;
  @Context
  protected ServletContext servletContext;


  // TODO(avaliani): re-add support for list resources using GenericEntity.
  /*
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<T> getResources() {
    return BaseDao.loadAll(getResourceClass());
  }
  */

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(U resourceView) {
    T resource = resourceView.getDao();
    preProcessUpsert(resource);
    BaseDao.upsert(resource);
    URI uri = uriInfo.getAbsolutePathBuilder().path(resource.getKey()).build();
    return Response.created(uri).build();
  }

  @Path("{resource}")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource(@PathParam("resource") String key) {
    return Response.ok(createBaseDaoView(getResourceObj(key))).build();
  }

  protected T getResourceObj(String keyStr) {
    Key<T> key = OfyUtil.<T>createKey(keyStr);
    T resource = ofy().load().key(key).now();
    if (resource == null) {
      throw ErrorResponseMsg.createException("resource does not exist", ErrorInfo.Type.BAD_REQUEST);
    }
    return resource;
  }

  protected abstract U createBaseDaoView(T resource);

  @Path("{resource}")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateResource(@PathParam("resource") String key, U resourceView) {
    T resource = resourceView.getDao();
    preProcessUpsert(resource);
    if (!resource.isKeyComplete()) {
      throw ErrorResponseMsg.createException("the resource key is incomplete",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (!key.equals(Key.create(resource).getString())) {
      throw ErrorResponseMsg.createException(
        format("the resource key [%s] does not match the url path key [%s]",
          Key.create(resource).getString(), key),
        ErrorInfo.Type.BAD_REQUEST);
    }
    BaseDao.upsert(resource);
    return Response.created(uriInfo.getAbsolutePath()).build();
  }

  @Path("{resource}")
  @DELETE
  public void deleteResource(@PathParam("resource") String keyStr) {
    BaseDao.delete(OfyUtil.<T>createKey(keyStr));
  }

  public void deleteResource(Key<T> key) {
    BaseDao.delete(key);
  }

  // protected abstract Class<T> getResourceClass();

  protected void preProcessUpsert(T resource) {
    // No-op.
  }
}
