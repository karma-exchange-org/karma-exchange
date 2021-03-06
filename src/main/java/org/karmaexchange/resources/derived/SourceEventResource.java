package org.karmaexchange.resources.derived;

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
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.resources.EventResource;
import org.karmaexchange.resources.msg.EventView;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminSubtask;
import org.karmaexchange.util.AdminUtil.AdminTaskType;

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
      @QueryParam("org_id") String orgId,
      @QueryParam("org_secret") String orgSecret,
      List<SyncRequest> syncRequests) {
    EventSourceInfo sourceInfo =
        EventSourceInfo.validateOrgSecret(orgId, orgSecret);

    AdminUtil.executeSubtaskAsAdmin(
      AdminTaskType.SOURCE_EVENT_UPDATE,
      new SyncEventsAdminSubtask(syncRequests, sourceInfo));
  }

  @Data
  private class SyncEventsAdminSubtask implements AdminSubtask {

    private final List<SyncRequest> syncRequests;
    private final EventSourceInfo sourceInfo;

    @Override
    public void execute() {
      // TODO(avaliani): Optimize this by doing a batch upsert / delete.
      for (SyncRequest syncRequest : syncRequests) {

        if (syncRequest.action == SyncRequest.Action.UPSERT) {

          if (syncRequest.sourceEvent != null) {
            Event event = syncRequest.sourceEvent.toEvent(sourceInfo);
            getEventResource().upsertResource(
              new EventView(event, false));
          } else if (syncRequest.sourceUser != null) {
            syncRequest.sourceUser.upsert(sourceInfo);
          } else if (syncRequest.sourceConfig != null) {
            syncRequest.sourceConfig.upsert(sourceInfo);
          }

        } else { // DELETE

          if (syncRequest.sourceEventId != null) {
            getEventResource().deleteResource(
              SourceEvent.createKey(sourceInfo, syncRequest.sourceEventId));
          }

        }

      }
    }
  }

  private EventResource getEventResource() {
    return new EventResource(uriInfo, request, servletContext);
  }

  @XmlRootElement
  @Data
  public static class SyncRequest {
    public enum Action {
      UPSERT,
      DELETE
    }

    private Action action;

    /**
     * Source db key of the event being synchronized. Only non-null for DELETE.
     */
    @Nullable
    private String sourceEventId;

    /**
     * Source db event of being synchronized. Only non-null for UPSERT.
     */
    @Nullable
    private SourceEvent sourceEvent;

    /**
     * Source db user being synchronized. Only non-null for UPSERT.
     */
    @Nullable
    private SourceUser sourceUser;

    /**
     * Source configuration info. Only non-null for UPSERT.
     */
    @Nullable
    private SourceConfig sourceConfig;
  }
}
