package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.EventComment;

@Path("/event_comment")
public class EventCommentResource extends BaseResource<EventComment> {

  @Override
  protected Class<EventComment> getResourceClass() {
    return EventComment.class;
  }

  @Override
  protected long getResourceId(EventComment eventComment) {
    return (eventComment.getId() == null) ? 0 : eventComment.getId();
  }
}
