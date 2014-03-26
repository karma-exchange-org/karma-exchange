package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.resources.OrganizationResource.MIN_ROLE_PARAM;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization.Role;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMembershipView;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@Path("/user")
public class UserResource extends ViewlessBaseDaoResourceEx<User> {

  // TODO(avaliani): Lock down user class. Some fields are only visible to owners of the
  //    user class.

  @Override
  protected void preProcessUpsert(User user) {
    if (!user.isKeyComplete()) {
      user.initKey();
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResources() {
    // TODO(avaliani): When we start using this api we should sort the results and therefore
    //     modify the query.
    PaginatedQuery.Builder<User> queryBuilder =
        PaginatedQuery.Builder.create(User.class, uriInfo, DEFAULT_NUM_SEARCH_RESULTS);
    ListResponseMsg<User> users = ListResponseMsg.create(queryBuilder.build().execute());
    return Response.ok(new GenericEntity<ListResponseMsg<User>>(users) {}).build();
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
    ConditionFilter participantFilter =
        new ConditionFilter(Event.getParticipantPropertyName(), userKey);
    return EventResource.eventSearch(uriInfo, Lists.newArrayList(participantFilter), userKey);
  }

  @Path("{user_key}/user_managed_event")
  public UserManagedEventResource getUserManagedEvents(@PathParam("user_key") String userKeyStr) {
    Key<User> userKey = OfyUtil.<User>createKey(userKeyStr);
    return new UserManagedEventResource(uriInfo, request, servletContext, userKey);
  }

  @Path("{user_key}/org")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMembershipView> getOrgs(
      @PathParam("user_key") String userKeyStr) {
    return getOrgs(OfyUtil.<User>createKey(userKeyStr), uriInfo);
  }

  public static ListResponseMsg<OrganizationMembershipView> getOrgs(Key<User> userKey,
      UriInfo uriInfo) {
    MultivaluedMap<String, String> reqParams = uriInfo.getQueryParameters();
    Role minRole = reqParams.containsKey(MIN_ROLE_PARAM) ?
        Role.valueOf(reqParams.getFirst(MIN_ROLE_PARAM)) : Role.ORGANIZER;

    User user = ofy().load().key(userKey).now();
    // For now we always fetch all the organizations. Implementing offsetted results requires
    // fetching all the organizations and sorting them by name. So it doesn't save us anything
    // to return a smaller batch at a time.
    return ListResponseMsg.create(OrganizationMembershipView.create(user, minRole));
  }
}
