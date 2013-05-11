package org.karmaexchange.util;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;

import com.googlecode.objectify.Key;

public final class UserService {

  private static final ThreadLocal<Key<User>> currentUser = new ThreadLocal<Key<User>>();
  private static final ThreadLocal<OAuthCredential> currentUserCredential =
      new ThreadLocal<OAuthCredential>();

  public static Key<User> getCurrentUserKey() {
    return currentUser.get();
  }

  public static void setCurrentUserKey(Key<User> user) {
    currentUser.set(user);
  }

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    return ofy().load().key(getCurrentUserKey()).now();
  }

  public static OAuthCredential getCurrentUserCredential() {
    return currentUserCredential.get();
  }

  public static void setCurrentUserCredential(OAuthCredential credential) {
    currentUserCredential.set(credential);
  }

  // boolean isUserAdmin()

  // Static utility methods only.
  private UserService() {
  }
}
