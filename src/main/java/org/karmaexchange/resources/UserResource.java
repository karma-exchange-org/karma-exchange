package org.karmaexchange.resources;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;
import org.karmaexchange.util.PaginationParam;
import org.karmaexchange.util.PaginatedQuery.FilterQueryClause;

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
      @PathParam("user_key") String userKeyStr,
      @QueryParam(EventResource.SEARCH_TYPE_PARAM) EventSearchType searchType,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_SEARCH_RESULTS) int limit,
      @QueryParam(EventResource.START_TIME_PARAM) Long startTimeValue,
      @QueryParam(EventResource.PARTICIPANT_TYPE_PARAM) ParticipantType participantType) {
    return userEventSearch(uriInfo, Key.<User>create(userKeyStr), searchType, afterCursorStr,
      limit, startTimeValue, participantType);
  }

  public static ListResponseMsg<EventSearchView> userEventSearch(UriInfo uriInfo, Key<User> userKey,
      @Nullable EventSearchType searchType, @Nullable String afterCursorStr, int limit,
      @Nullable Long startTimeValue, @Nullable ParticipantType participantType) {
    FilterQueryClause participantFilter;
    boolean loadReviews = userKey.equals(getCurrentUserKey());
    if (participantType == null) {
      participantFilter = new FilterQueryClause(Event.getParticipantPropertyName(), userKey);
    } else {
      participantFilter =
          new FilterQueryClause(Event.getParticipantPropertyName(participantType), userKey);
      participantFilter.setPaginationParam(
        new PaginationParam(EventResource.PARTICIPANT_TYPE_PARAM, participantType.toString()));
    }
    return EventResource.eventSearch(afterCursorStr, limit, startTimeValue,
      uriInfo.getAbsolutePath(), searchType, Lists.newArrayList(participantFilter), loadReviews);
  }
}
