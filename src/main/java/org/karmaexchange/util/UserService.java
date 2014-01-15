package org.karmaexchange.util;

import static org.karmaexchange.util.AdminUtil.isAdminKey;
import static org.karmaexchange.util.OfyService.ofy;

import javax.annotation.Nullable;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;

import com.googlecode.objectify.Key;

public final class UserService {

  private static final Key<User> NOT_LOGGED_IN_KEY =
      ReservedKeyType.createReservedKey(ReservedKeyType.NOT_LOGGED_IN);

  private static final ThreadLocal<Key<User>> currentUserKey = new ThreadLocal<Key<User>>();
  private static final ThreadLocal<OAuthCredential> currentUserCredential =
      new ThreadLocal<OAuthCredential>();

  public enum ReservedKeyType {
    ADMIN,
    NOT_LOGGED_IN;

    private static final String RESERVED_KEY_TYPE_SEPERATOR = ":";

    public static Key<User> createReservedKey(ReservedKeyType type) {
      return createReservedKey(type, null);
    }

    public static Key<User> createReservedKey(ReservedKeyType type, @Nullable String subType) {
      if (subType == null) {
        return Key.create(User.class, type.toString());
      } else {
        return Key.create(User.class, type + RESERVED_KEY_TYPE_SEPERATOR + subType);
      }
    }

    public static boolean isReservedKey(Key<User> key, ReservedKeyType type) {
      return key.getName().startsWith(type.toString());
    }
  }

  public static Key<User> getCurrentUserKey() {
    return (currentUserKey.get() == null) ? NOT_LOGGED_IN_KEY : currentUserKey.get();
  }

  // Objectify caches this in the session cache.
  public static User getCurrentUser() {
    User user = ofy().load().key(getCurrentUserKey()).now();
    if (user == null) {
      SocialNetworkProvider socialNetworkProvider =
          SocialNetworkProviderFactory.getProvider(getCurrentUserCredential());
      user = User.persistNewUser(socialNetworkProvider.createUser());
    }
    return user;
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

  public static boolean isNotLoggedInUser() {
    return getCurrentUserKey() == NOT_LOGGED_IN_KEY;
  }

  // Static utility methods only.
  private UserService() {
  }
}
