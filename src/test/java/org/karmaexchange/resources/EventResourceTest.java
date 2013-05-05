package org.karmaexchange.resources;

import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.util.OfyService;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;

/*
 * TODO(avaliani): this test is a work in progress.
 */
public class EventResourceTest extends JerseyTest {

  private final LocalServiceTestHelper appEngineHelper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  public EventResourceTest() {
    super("org.karmaexchange.resources");
  }

  @Before
  public void setUp() throws Exception {
    appEngineHelper.setUp();
    // Register the objectify classes.
    OfyService.ofy();
  }

  @After
  public void tearDown() throws Exception {
    appEngineHelper.tearDown();
  }

  @Test
  public void testGetUsers() {
    /*
    WebResource webResource = resource();
    String responseMsg = webResource.path("user").accept(
      MediaType.APPLICATION_JSON).get(String.class);
    System.out.println(responseMsg);
    */
  }

}
