package org.karmaexchange.resources.derived;

import static org.karmaexchange.util.OfyService.ofy;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import lombok.Data;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.derived.SourceEventGeneratorInfo;
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
  public Response upsertResource(
      @QueryParam("org_key") String orgKeyStr,
      @QueryParam("org_secret") String orgSecret,
      SourceEvent sourceEvent) {
    Key<Organization> orgKey = OfyUtil.createKey(orgKeyStr);
    validateOrgSecret(orgKey, orgSecret);

    // Do this outside of the transaction since it can be slow due to dynamic geocoding.
    Event event = sourceEvent.toEvent(orgKey);

    UpsertAdminSubtask upsertTask = new UpsertAdminSubtask(event);
    AdminUtil.executeSubtaskAsAdmin(
      AdminTaskType.SOURCE_EVENT_UPDATE, upsertTask);
    return upsertTask.response;
  }

  @Data
  private class UpsertAdminSubtask implements AdminSubtask {

    private final Event event;
    private Response response;

    @Override
    public void execute() {
      // TODO(avaliani): fix return key. Currently it contains derived in the path.
      response = getEventResource().upsertResource(new EventView(event, false));
    }
  }

  @Path("{source_key}")
  @DELETE
  public void deleteResource(
      final @PathParam("source_key") String sourceKey,
      @QueryParam("org_key") String orgKeyStr,
      @QueryParam("org_secret") String orgSecret) {
    final Key<Organization> orgKey = OfyUtil.createKey(orgKeyStr);
    validateOrgSecret(orgKey, orgSecret);

    AdminUtil.executeSubtaskAsAdmin(
      AdminTaskType.SOURCE_EVENT_UPDATE,
      new AdminSubtask() {

        @Override
        public void execute() {
          getEventResource().deleteResource(SourceEvent.createKey(orgKey, sourceKey));
        }

      });
  }

  private static void validateOrgSecret(Key<Organization> orgKey, String orgSecret) {
    SourceEventGeneratorInfo config =
        ofy().load().key(SourceEventGeneratorInfo.createKey(orgKey)).now();
    if (config == null) {
      throw ErrorResponseMsg.createException(
        "organization is not configured to support derived events", ErrorInfo.Type.BAD_REQUEST);
    }
    if (!config.getSecret().equals(orgSecret)) {
      throw ErrorResponseMsg.createException(
        "event source authentication credentials are not valid",
        ErrorInfo.Type.AUTHENTICATION);
    }
  }

  private EventResource getEventResource() {
    return new EventResource(uriInfo, request, servletContext);
  }
}
