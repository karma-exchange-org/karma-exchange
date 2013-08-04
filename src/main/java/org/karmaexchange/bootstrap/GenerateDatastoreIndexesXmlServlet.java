package org.karmaexchange.bootstrap;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.RequestStatus;
import org.karmaexchange.resources.EventResource;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.resources.OrganizationResource;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.util.AdminTaskServlet;
import org.karmaexchange.util.AdminUtil;

import com.googlecode.objectify.Key;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@SuppressWarnings("serial")
public class GenerateDatastoreIndexesXmlServlet extends AdminTaskServlet {
  private static final Logger logger =
      Logger.getLogger(GenerateDatastoreIndexesXmlServlet.class.getName());

  private PrintWriter statusWriter;

  public GenerateDatastoreIndexesXmlServlet() {
    super(AdminUtil.AdminTaskType.BOOTSTRAP);
  }

  @Override
  public void execute() throws IOException {
    resp.setContentType("text/plain");
    statusWriter = resp.getWriter();

    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource service = client.resource(getBaseUri());

    statusWriter.println("Issuing queries to build datastore-indexes.xml file...");

    issueGetRequestAndCheckRespone(
      service.path("api/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString()));

    issueGetRequestAndCheckRespone(
      service.path("api/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString())
      .queryParam(EventResource.KEYWORDS_PARAM, "animals"));
    // Used for past-event search of events associated with an organization.
    issueGetRequestAndCheckRespone(
      service.path("api/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.PAST.toString())
      .queryParam(EventResource.KEYWORDS_PARAM, "animals"));

    Key<Event> arbitraryEventKey = getArbitraryEventKey();
    issueGetRequestAndCheckRespone(
      service.path("api/event")
      .path(arbitraryEventKey.getString())
      .path("review_comment_view"));

    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString()));
    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.PAST.toString()));

    // Not currently used by the UI.
    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString())
      .queryParam(EventResource.PARTICIPANT_TYPE_PARAM, ParticipantType.ORGANIZER.toString()));
    // Not currently used by the UI.
    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.PAST.toString())
      .queryParam(EventResource.PARTICIPANT_TYPE_PARAM, ParticipantType.ORGANIZER.toString()));
    // Seeing upcoming or past events by registered / wait listed doesn't seem as useful.
    // Just the full list of events will do in that case.

    // Not currently used by the UI.
    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString())
      .queryParam(EventResource.KEYWORDS_PARAM, "animals"));
    // Not currently used by the UI.
    issueGetRequestAndCheckRespone(
      service.path("api/me/event")
      .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.PAST.toString())
      .queryParam(EventResource.KEYWORDS_PARAM, "animals"));

    Key<Organization> arbitraryOrgKey = getArbitraryOrganizationKey();
    issueGetRequestAndCheckRespone(
      service.path("api/org")
      .path(arbitraryOrgKey.getString())
      .path("member"));
    issueGetRequestAndCheckRespone(
      service.path("api/org")
      .path(arbitraryOrgKey.getString())
      .path("member")
      .queryParam(OrganizationResource.ROLE_PARAM, Organization.Role.ADMIN.toString()));
    issueGetRequestAndCheckRespone(
      service.path("api/org")
      .path(arbitraryOrgKey.getString())
      .path("member")
      .queryParam(OrganizationResource.ROLE_PARAM, Organization.Role.ORGANIZER.toString()));
    issueGetRequestAndCheckRespone(
      service.path("api/org")
      .path(arbitraryOrgKey.getString())
      .path("member")
      .queryParam(OrganizationResource.MEMBERSHIP_STATUS_PARAM, RequestStatus.PENDING.toString()));

    issueGetRequestAndCheckRespone(
      service.path("task/process_event_completions"));

    statusWriter.println("Completed issuing queries.");
  }

  private URI getBaseUri() {
    try {
      return new URI(req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void issueGetRequestAndCheckRespone(WebResource resource) {
    ClientResponse response = issueGetRequest(resource);
    Response.Status status = Response.Status.fromStatusCode(response.getStatus());
    if (!status.equals(Response.Status.OK)) {
      logger.severe("ERROR: " + response);
      if (status.equals(Response.Status.BAD_REQUEST) && response.hasEntity()) {
        ErrorResponseMsg errorMsg = response.getEntity(ErrorResponseMsg.class);
        logger.severe("  " + errorMsg);
      }
      throw new RuntimeException(response.toString());
    }
  }

  private ClientResponse issueGetRequest(WebResource resource) {
    Cookie[] cookies = req.getCookies();
    WebResource.Builder resourceBldr = resource.accept(MediaType.APPLICATION_JSON);
    for (Cookie cookie : cookies) {
      resourceBldr = resourceBldr.cookie(
        new javax.ws.rs.core.Cookie(cookie.getName(), cookie.getValue(), cookie.getPath(),
          cookie.getDomain(), cookie.getVersion()));
    }
    return resourceBldr.get(ClientResponse.class);
  }

  private Key<Event> getArbitraryEventKey() {
    Iterator<Key<Event>> keysIter = ofy().load().type(Event.class).limit(1).keys().iterator();
    if (keysIter.hasNext()) {
      return keysIter.next();
    } else {
      throw new RuntimeException(
        "At least one event is required in order to generate the datastore-indexes.xml ");
    }
  }

  private Key<Organization> getArbitraryOrganizationKey() {
    Iterator<Key<Organization>> keysIter =
        ofy().load().type(Organization.class).limit(1).keys().iterator();
    if (keysIter.hasNext()) {
      return keysIter.next();
    } else {
      throw new RuntimeException(
        "At least one organization is required in order to generate the datastore-indexes.xml ");
    }
  }
}
