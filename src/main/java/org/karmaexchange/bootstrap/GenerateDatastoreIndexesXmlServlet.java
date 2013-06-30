package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.MediaType;

import org.karmaexchange.resources.EventResource;
import org.karmaexchange.resources.EventResource.EventSearchType;
import org.karmaexchange.util.AdminTaskServlet;
import org.karmaexchange.util.AdminUtil;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@SuppressWarnings("serial")
public class GenerateDatastoreIndexesXmlServlet extends AdminTaskServlet {
  private static final Logger logger =
      Logger.getLogger(GenerateDatastoreIndexesXmlServlet.class.getName());

  public GenerateDatastoreIndexesXmlServlet() {
    super(AdminUtil.AdminTaskType.BOOTSTRAP);
  }

  @Override
  public void execute() throws IOException {
    resp.setContentType("text/plain");
    PrintWriter statusWriter = resp.getWriter();

    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource service = client.resource(getBaseUri());

    statusWriter.println("Issuing queries to build datastore-indexes.xml file...");

    Cookie[] cookies = req.getCookies();
    WebResource.Builder resource = service.path("api").path("event")
        .queryParam(EventResource.SEARCH_TYPE_PARAM, EventSearchType.UPCOMING.toString())
        .accept(MediaType.APPLICATION_JSON);
    for (Cookie cookie : cookies) {
      resource = resource.cookie(new javax.ws.rs.core.Cookie(cookie.getName(), cookie.getValue(),
        cookie.getPath(), cookie.getDomain(), cookie.getVersion()));
    }
    ClientResponse response = resource.get(ClientResponse.class);

    // debug
    statusWriter.println(response);

    statusWriter.println("Completed issuing queries.");
  }

  private URI getBaseUri() {
    try {
      return new URI(req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
