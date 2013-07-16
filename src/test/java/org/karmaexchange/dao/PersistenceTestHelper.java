package org.karmaexchange.dao;

import static org.junit.Assert.assertEquals;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.TestUtil.debug;

import org.junit.After;
import org.junit.Before;
import org.karmaexchange.util.DatastoreTestUtil;
import org.karmaexchange.util.OfyService;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;

public class PersistenceTestHelper {

  private final LocalServiceTestHelper appEngineHelper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

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

  protected <T> void validatePersistence(T entity) throws Exception {
    if (debug) {
      System.out.println("Before: " + entity);
      System.out.println("Before datastore entity: ");
      DatastoreTestUtil.dumpEntity(entity);
    }

    Key<T> key = ofy().save().entity(entity).now();
    T persistedEntity = ofy().load().key(key).now();

    if (debug) {
      System.out.println("After: " + persistedEntity);
      System.out.println("After datastore  entity: ");
      DatastoreTestUtil.dumpEntity(entity);
    }

    assertEquals(entity, persistedEntity);
  }
}
