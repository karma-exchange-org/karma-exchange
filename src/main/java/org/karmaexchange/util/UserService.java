package org.karmaexchange.util;

import javax.annotation.Nullable;

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

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    return BaseDao.load(getCurrentUserKey());
  }

  public static OAuthCredential getCurrentUserCredential() {
    return currentUserCredential.get();
  }

  public static void updateCurrentUser(@Nullable OAuthCredential credential,
                                       @Nullable Key<User> user) {
    currentUserCredential.set(credential);
    currentUserKey.set(user);
  }

  // boolean isUserAdmin()

  // Static utility methods only.
  private UserService() {
  }
}
