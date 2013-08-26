package org.karmaexchange.dao;

import static org.junit.Assert.assertEquals;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.TestUtil.DEBUG;

import org.junit.After;
import org.junit.Before;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminTaskType;
import org.karmaexchange.util.DatastoreTestUtil;
import org.karmaexchange.util.OfyService;
import org.karmaexchange.util.UserService;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;

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

  protected <T extends BaseDao<T>> T validatePersistence(T entity) throws Exception {
    AdminUtil.setCurrentUser(AdminTaskType.TEST);
    try {
      if (DEBUG) {
        System.out.println("Before: " + entity);
        System.out.println("Before datastore entity: ");
        DatastoreTestUtil.dumpEntity(entity);
      }

      final Key<T> key = ofy().save().entity(entity).now();

      // Bypass the Objectify session cache to see how the entity is actually persisted on disk.
      T persistedEntity = ofy().transact(new Work<T>() {
        public T run() {
          return ofy().load().key(key).now();
        }
      });

      if (DEBUG) {
        System.out.println("After: " + persistedEntity);
        System.out.println("After datastore  entity: ");
        DatastoreTestUtil.dumpEntity(entity);
      }

      // Force a process load of the entity to pass the equality test.
      entity.processLoad();
      assertEquals(entity, persistedEntity);
      return persistedEntity;
    } finally {
      UserService.clearCurrentUser();
    }
  }
}
