package org.karmaexchange.resources;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMembershipView;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@Path("/user")
public class UserResource extends BaseDaoResource<User> {

  @Override
  protected Class<User> getResourceClass() {
    return User.class;
  }

  @Override
  protected void preProcessUpsert(User user) {
    if (!user.isKeyComplete()) {
      user.initKey();
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<User> getResources() {
    return BaseDao.loadAll(getResourceClass());
  }

  @Path("{user_key}/event")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getEvents(
      @PathParam("user_key") String userKeyStr) {
    return userEventSearch(uriInfo, OfyUtil.<User>createKey(userKeyStr));
  }

  public static ListResponseMsg<EventSearchView> userEventSearch(UriInfo uriInfo,
      Key<User> userKey) {
    boolean loadReviews = userKey.equals(getCurrentUserKey());
    ConditionFilter participantFilter =
        new ConditionFilter(Event.getParticipantPropertyName(), userKey);
    return EventResource.eventSearch(uriInfo, Lists.newArrayList(participantFilter), loadReviews);
  }

  @Path("{user_key}/org")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMembershipView> getOrgs(
      @PathParam("user_key") String userKeyStr) {
    return getOrgs(OfyUtil.<User>createKey(userKeyStr));
  }

  public static ListResponseMsg<OrganizationMembershipView> getOrgs(Key<User> userKey) {
    User user = BaseDao.load(userKey);
    // For now we always fetch all the organizations. Implementing offsetted results requires
    // fetching all the organizations and sorting them by name. So it doesn't save us anything
    // to return a smaller batch at a time.
    return ListResponseMsg.create(OrganizationMembershipView.create(user));
  }
}
