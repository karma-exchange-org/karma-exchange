package org.karmaexchange.resources.derived;

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

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.derived.SourceEvent;
import org.karmaexchange.resources.EventResource;
import org.karmaexchange.resources.msg.EventView;
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
    Key<Organization> orgKey = validateOrgSecret(orgKeyStr, orgSecret);
    Event event = sourceEvent.toEvent(orgKey);
    // TODO(avaliani): fix return key
    return getEventResource().upsertResource(new EventView(event, false));
  }

  @Path("{source_key}")
  @DELETE
  public void deleteResource(
      @PathParam("source_key") String sourceKey,
      @QueryParam("org_key") String orgKeyStr,
      @QueryParam("org_secret") String orgSecret) {
    Key<Organization> orgKey = validateOrgSecret(orgKeyStr, orgSecret);
    getEventResource().deleteResource(SourceEvent.createKey(orgKey, sourceKey));
  }

  private Key<Organization> validateOrgSecret(String orgKeyStr, String orgSecret) {
    // TODO(avaliani): Validate orgSecret is correct for orgKey
    return OfyUtil.createKey(orgKeyStr);
  }

  private EventResource getEventResource() {
    return new EventResource(uriInfo, request, servletContext);
  }
}
