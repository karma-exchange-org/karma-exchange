package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import org.karmaexchange.dao.User;
import org.karmaexchange.util.UserService.ReservedKeyType;

import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

public class AdminUtil {

  public enum AdminTaskType {
    TEST,
    BOOTSTRAP,
    AUTH_RESOURCE,
    ADMIN_MANAGED_RESOURCE,
    TASK_QUEUE,
    MAP_REDUCE,
    REGISTRATION,
    SOURCE_EVENT_UPDATE;

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
    UserService.setCurrentUser(type.getKey());
  }

  public static void executeSubtaskAsAdmin(AdminTaskType taskType, AdminSubtask subtask) {
    Key<User> prevKey = UserService.getCurrentUserKey();
    setCurrentUser(taskType);
    try {
      subtask.execute();
    } finally {
      UserService.setCurrentUser(prevKey);
    }
  }

  public static void executeSubtaskAsUser(Key<User> userKey, AdminSubtask subtask) {
    Key<User> prevAdminKey = UserService.getCurrentUserKey();
    checkState(isAdminKey(prevAdminKey));
    UserService.setCurrentUser(userKey);
    try {
      subtask.execute();
    } finally {
      UserService.setCurrentUser(prevAdminKey);
    }
  }

  public interface AdminSubtask {
    void execute();
  }
}
