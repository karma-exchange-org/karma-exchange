package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.User;

@Path("/user")
public class UserResource extends BaseDaoResource<User> {

  @Override
  protected Class<User> getResourceClass() {
    return User.class;
  }
}
