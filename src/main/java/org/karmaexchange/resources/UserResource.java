package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.User;

@Path("/user")
public class UserResource extends BaseResource<User> {

  @Override
  protected Class<User> getResourceClass() {
    return User.class;
  }

  @Override
  protected long getResourceId(User user) {
    return user.getId();
  }
}
