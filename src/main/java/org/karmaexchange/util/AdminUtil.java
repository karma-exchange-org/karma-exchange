package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import javax.annotation.Nullable;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.util.UserService.ReservedKeyType;

import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

public class AdminUtil {

  public enum AdminTaskType {
    TEST,
    BOOTSTRAP,
    OAUTH_FILTER,
    TASK_QUEUE,
    MAP_REDUCE,
    REGISTRATION;

    public Key<User> getKey() {
      return ReservedKeyType.createReservedKey(UserService.ReservedKeyType.ADMIN, name());
    }
  }

  private static final Set<Key<User>> adminKeys = Sets.newHashSet();
  static {
    for (AdminTaskType type : AdminTaskType.values()) {
      adminKeys.add(type.getKey());
    }
  }

  public static boolean isAdminKey(Key<User> key) {
    return ReservedKeyType.isReservedKey(key, UserService.ReservedKeyType.ADMIN);
  }

  public static void setCurrentUser(AdminTaskType type) {
    UserService.setCurrentUser(null, type.getKey());
  }

  public static void executeSubtaskAsUser(Key<User> userKey, @Nullable OAuthCredential credential,
      AdminSubtask subtask) {
    Key<User> prevAdminKey = UserService.getCurrentUserKey();
    // PrevCredential is currently null for admin tasks by default.
    OAuthCredential prevCredential = UserService.getCurrentUserCredential();
    checkState(isAdminKey(prevAdminKey));
    UserService.setCurrentUser(credential, userKey);
    try {
      subtask.execute();
    } finally {
      UserService.setCurrentUser(prevCredential, prevAdminKey);
    }
  }

  public interface AdminSubtask {
    void execute();
  }
}
