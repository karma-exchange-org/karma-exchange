package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.UserUsage;
import org.karmaexchange.dao.UserUsage.UserAccess;

import com.google.common.collect.Maps;

@Path("/admin/user_usage")
public class UserUsageResource {

  @Context
  protected UriInfo uriInfo;

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<UserUsage> getResources() {
    List<UserUsage> usages = ofy().load().type(UserUsage.class).list();
    List<UserAccess> accesses = ofy().load().type(UserAccess.class).list();

    Map<String, UserUsage> usagesMap = Maps.newHashMap();
    for (UserUsage usage : usages) {
      usagesMap.put(usage.getOwner(), usage);
    }
    for (UserAccess access : accesses) {
      UserUsage usage = usagesMap.get(access.getOwner().getString());
      if (usage != null) {
        usage.setLastVisited(access.getLastVisited());
      }
    }

    return usages;
  }

}
