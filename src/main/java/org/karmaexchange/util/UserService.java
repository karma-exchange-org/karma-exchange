package org.karmaexchange.util;

import org.karmaexchange.dao.BaseDao;
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

  public static void setCurrentUserKey(Key<User> user) {
    currentUserKey.set(user);
  }

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    return BaseDao.load(getCurrentUserKey());
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
