package org.karmaexchange.util;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Key;

public final class DatastoreTestUtil {

  public static <T> void dumpEntity(T object) throws EntityNotFoundException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity e = datastore.get(KeyFactory.stringToKey(Key.<T>create(object).getString()));
    System.out.println(e);
  }
}
