package org.karmaexchange.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.karmaexchange.dao.User;

@Path("/user")
public class UserResource extends BaseDaoResource<User> {

  public UserResource(@Context HttpServletRequest servletRequest) {
    super(servletRequest);
  }

  @Override
  protected Class<User> getResourceClass() {
    return User.class;
  }
}
