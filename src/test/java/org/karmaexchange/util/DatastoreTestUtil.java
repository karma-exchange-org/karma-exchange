package org.karmaexchange.util;

import static org.karmaexchange.util.OfyService.ofy;

public final class DatastoreTestUtil {

  public static <T> void dumpEntity(T object) {
    // DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    // Entity e = datastore.get(KeyFactory.stringToKey(Key.<T>create(object).getString()));
    System.out.println(ofy().toEntity(object));
  }
}
