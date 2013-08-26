package org.karmaexchange.util;

import static org.karmaexchange.util.AdminUtil.isAdminKey;
import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;

import com.googlecode.objectify.Key;

public final class UserService {

  private static final ThreadLocal<Key<User>> currentUserKey = new ThreadLocal<Key<User>>();
  private static final ThreadLocal<OAuthCredential> currentUserCredential =
      new ThreadLocal<OAuthCredential>();

  public static Key<User> getCurrentUserKey() {
    return currentUserKey.get();
  }

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    return ofy().load().key(getCurrentUserKey()).now();
  }

  public static OAuthCredential getCurrentUserCredential() {
    return currentUserCredential.get();
  }

  public static void setCurrentUser(OAuthCredential credential, Key<User> user) {
    currentUserCredential.set(credential);
    currentUserKey.set(user);
  }

  public static void clearCurrentUser() {
    currentUserCredential.set(null);
    currentUserKey.set(null);
  }

  public static boolean isCurrentUserAdmin() {
    return isAdminKey(getCurrentUserKey());
  }

  // Static utility methods only.
  private UserService() {
  }
}
