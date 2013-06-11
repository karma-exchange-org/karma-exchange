package org.karmaexchange.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;

import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

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

  @Override
  protected void preProcessUpsert(User user) {
    if (!user.isKeyComplete()) {
      user.initKey();
    }
  }

  @Path("{user_key}/event")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getEvents(
      @PathParam("user_key") String userKeyStr,
      @QueryParam(EventResource.SEARCH_TYPE_PARAM) EventSearchType searchType,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_SEARCH_RESULTS) int limit,
      @QueryParam(EventResource.START_TIME_PARAM) Long startTimeValue) {
    Map<String, Object> filters = Maps.newHashMap();
    filters.put("participants.user.key", Key.<User>create(userKeyStr));
    return EventResource.eventSearch(afterCursorStr, limit, startTimeValue,
      uriInfo.getAbsolutePath(), searchType, filters);
  }
}
