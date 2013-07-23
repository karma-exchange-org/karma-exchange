package org.karmaexchange.resources;

import static org.karmaexchange.util.UserService.getCurrentUserKey;
import static org.karmaexchange.resources.EventResource.PARTICIPANT_TYPE_PARAM;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
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
    return userEventSearch(uriInfo, Key.<User>create(userKeyStr));
  }

  public static ListResponseMsg<EventSearchView> userEventSearch(UriInfo uriInfo,
      Key<User> userKey) {
    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
    ParticipantType participantType = queryParams.containsKey(PARTICIPANT_TYPE_PARAM) ?
        ParticipantType.valueOf(queryParams.getFirst(PARTICIPANT_TYPE_PARAM)) : null;
    ConditionFilter participantFilter;
    boolean loadReviews = userKey.equals(getCurrentUserKey());
    if (participantType == null) {
      participantFilter = new ConditionFilter(Event.getParticipantPropertyName(), userKey);
    } else {
      participantFilter =
          new ConditionFilter(Event.getParticipantPropertyName(participantType), userKey);
    }
    return EventResource.eventSearch(uriInfo, Lists.newArrayList(participantFilter), loadReviews);
  }
}
