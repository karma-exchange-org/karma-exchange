package org.karmaexchange.resources.derived;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.resources.EventResource;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.EventView;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminSubtask;
import org.karmaexchange.util.AdminUtil.AdminTaskType;
import org.karmaexchange.util.OfyUtil;

import com.googlecode.objectify.Key;

@Path("/derived/event")
public class SourceEventResource {

  @Context
  protected UriInfo uriInfo;
  @Context
  protected Request request;
  @Context
  protected ServletContext servletContext;

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public void syncEvents(
      @QueryParam("org_key") String orgKeyStr,
      @QueryParam("org_secret") String orgSecret,
      List<EventSyncRequest> syncRequests) {
    Key<Organization> orgKey = OfyUtil.createKey(orgKeyStr);
    EventSourceInfo sourceInfo = validateOrgSecret(orgKey, orgSecret);

    AdminUtil.executeSubtaskAsAdmin(
      AdminTaskType.SOURCE_EVENT_UPDATE,
      new SyncEventsAdminSubtask(syncRequests, sourceInfo));
  }

  @Data
  private class SyncEventsAdminSubtask implements AdminSubtask {

    private final List<EventSyncRequest> syncRequests;
    private final EventSourceInfo sourceInfo;

    @Override
    public void execute() {
      // TODO(avaliani): Optimize this by doing a batch upsert / delete.
      for (EventSyncRequest syncRequest : syncRequests) {
          if (syncRequest.action == EventSyncRequest.Action.UPSERT) {
            Event event = syncRequest.sourceEvent.toEvent(sourceInfo);
            getEventResource().upsertResource(
              new EventView(event, false));
          } else {
            getEventResource().deleteResource(
              SourceEvent.createKey(sourceInfo, syncRequest.sourceKey));
          }
      }
    }
  }

  private static EventSourceInfo validateOrgSecret(Key<Organization> orgKey,
      String orgSecret) {
    EventSourceInfo sourceInfo =
        ofy().load().key(EventSourceInfo.createKey(orgKey)).now();
    if (sourceInfo == null) {
      throw ErrorResponseMsg.createException(
        "organization is not configured to support derived events", ErrorInfo.Type.BAD_REQUEST);
    }
    if (!sourceInfo.getSecret().equals(orgSecret)) {
      throw ErrorResponseMsg.createException(
        "event source authentication credentials are not valid",
        ErrorInfo.Type.AUTHENTICATION);
    }
    return sourceInfo;
  }

  private EventResource getEventResource() {
    return new EventResource(uriInfo, request, servletContext);
  }

  @XmlRootElement
  @Data
  public static class EventSyncRequest {
    public enum Action {
      UPSERT,
      DELETE
    }

    private Action action;

    /**
     * Source db key of the event being synchronized. Non-null for the DELETE action.
     */
    @Nullable
    private String sourceKey;

    /**
     * Source db event of being synchronized. Non-null for the UPSERT action.
     */
    @Nullable
    private SourceEvent sourceEvent;

    public void execute(Key<Organization> orgKey) {
    }
  }
}
