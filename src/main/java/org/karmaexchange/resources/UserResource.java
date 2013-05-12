package org.karmaexchange.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.User;

@Path("/user")
public class UserResource extends BaseDaoResource<User> {

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<User> getResources() {
    return BaseDao.loadAll(getResourceClass());
  }

  @Override
  protected Class<User> getResourceClass() {
    return User.class;
  }
}
