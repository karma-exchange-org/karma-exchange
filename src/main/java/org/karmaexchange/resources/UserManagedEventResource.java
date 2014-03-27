package org.karmaexchange.resources;

import static java.lang.String.format;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserManagedEvent;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.FilterQueryClause;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;

public class UserManagedEventResource extends ViewlessBaseDaoResource<UserManagedEvent> {

  private Key<User> userKey;

  public UserManagedEventResource(UriInfo uriInfo, Request request,
      ServletContext servletContext, Key<User> userKey) {
    super(uriInfo, request, servletContext);
    this.userKey = userKey;
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<UserManagedEvent> getResources() {
    PaginatedQuery.Result<UserManagedEvent> queryResult =
        EventResource.eventBaseQuery(UserManagedEvent.class, uriInfo, null,
          ImmutableList.<FilterQueryClause>of(), userKey);
    return ListResponseMsg.create(queryResult);
  }

  @Override
  public Response upsertResource(UserManagedEvent resource) {
    if (!resource.isKeyComplete() && (resource.getOwner() == null)) {
      resource.setOwner(userKey.getString());
    }
    validateResource(resource);
    return super.upsertResource(resource);
  }

  @Override
  public Response updateResource(@PathParam("resource") String key, UserManagedEvent resource) {
    validateResource(resource);
    return super.updateResource(key, resource);
  }

  private void validateResource(UserManagedEvent resource) {
    Key<User> resourceOwnerKey = OfyUtil.createKey(resource.getOwner());
    if (!resourceOwnerKey.equals(userKey)) {
      throw ErrorResponseMsg.createException(
        format("the resource owner key [%s] does not match the user path key [%s]",
          resource.getOwner(), userKey.getString()),
        ErrorInfo.Type.BAD_REQUEST);
    }
  }
}
