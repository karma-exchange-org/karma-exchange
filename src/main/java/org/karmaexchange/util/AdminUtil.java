package org.karmaexchange.util;

import java.util.Set;

import org.karmaexchange.dao.User;

import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

public class AdminUtil {

  private static final String ADMIN_KEY_PREFIX = "ADMIN:";

  public enum AdminTaskType {
    OAUTH_FILTER {
      @Override
      public Key<User> getKey() {
        return createAdminKey(name());
      }
    },
    TASK_QUEUE {
      @Override
      public Key<User> getKey() {
        return createAdminKey(name());
      }
    };

    public abstract Key<User> getKey();

    private static Key<User> createAdminKey(String id) {
      return Key.create(User.class, ADMIN_KEY_PREFIX + id);
    }
  }

  private static final Set<Key<User>> adminKeys = Sets.newHashSet();
  static {
    for (AdminTaskType type : AdminTaskType.values()) {
      adminKeys.add(type.getKey());
    }
  }

  public static boolean isAdminKey(Key<User> key) {
    return adminKeys.contains(key);
  }

  public static void setCurrentUser(AdminTaskType type) {
    UserService.setCurrentUser(null, type.getKey());
  }
}
