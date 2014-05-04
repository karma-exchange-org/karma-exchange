package org.karmaexchange.util;

import static org.karmaexchange.util.AdminUtil.isAdminKey;
import static org.karmaexchange.util.OfyService.ofy;

import javax.annotation.Nullable;

import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public final class UserService {

  // We're creating a key hierarchy since it's not clear from the documentation if keys of the
  // same kind one with a numeric id and one with a string id collide. It is clear that keys that
  // have different parents should never collide.
  private static final Key<User> RESERVED_KEY_PARENT =
      Key.create(User.class, "RESERVED_KEY");

  private static final Key<User> NOT_LOGGED_IN_KEY =
      ReservedKeyType.createReservedKey(ReservedKeyType.NOT_LOGGED_IN);

  private static final ThreadLocal<Key<User>> currentUserKey = new ThreadLocal<Key<User>>();

  public enum ReservedKeyType {
    ADMIN,
    NOT_LOGGED_IN;

    private static final String RESERVED_KEY_TYPE_SEPERATOR = ":";

    public static Key<User> createReservedKey(ReservedKeyType type) {
      return createReservedKey(type, null);
    }

    public static Key<User> createReservedKey(ReservedKeyType type, @Nullable String subType) {
      if (subType == null) {
        return Key.create(RESERVED_KEY_PARENT, User.class, type.toString());
      } else {
        return Key.create(RESERVED_KEY_PARENT, User.class,
          type + RESERVED_KEY_TYPE_SEPERATOR + subType);
      }
    }

    public static boolean isReservedKey(Key<User> key, ReservedKeyType type) {
      return (key.getParent() != null) && key.getParent().equals(RESERVED_KEY_PARENT) &&
          key.getName().startsWith(type.toString());
    }
  }

  public static Key<User> getCurrentUserKey() {
    return (currentUserKey.get() == null) ? NOT_LOGGED_IN_KEY : currentUserKey.get();
  }

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    if (isNotLoggedInUser() || isCurrentUserAdmin()) {
      throw ErrorResponseMsg.createException("Login required", ErrorInfo.Type.LOGIN_REQUIRED);
    }
    User user = ofy().load().key(getCurrentUserKey()).now();
    if (user == null) {
      throw ErrorResponseMsg.createException("User does not exist", ErrorInfo.Type.LOGIN_REQUIRED);
    }
    return user;
  }

  public static void setCurrentUser(Key<User> user) {
    currentUserKey.set(user);
  }

  public static void clearCurrentUser() {
    currentUserKey.set(null);
  }

  public static boolean isCurrentUserAdmin() {
    return isAdminKey(getCurrentUserKey());
  }

  public static boolean isNotLoggedInUser() {
    return getCurrentUserKey() == NOT_LOGGED_IN_KEY;
  }

  // Static utility methods only.
  private UserService() {
  }
}
