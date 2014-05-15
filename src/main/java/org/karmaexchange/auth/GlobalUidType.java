package org.karmaexchange.auth;

public enum GlobalUidType {
  /*
   * Enums can't be extended. Therefore we create a parallel enum to AuthProviderType
   * to handle all the mapping types.
   */

  FACEBOOK,
  MOZILLA_PERSONA,
  EMAIL;

  public static GlobalUidType toGlobalUidType(AuthProviderType authProviderType) {
    return GlobalUidType.valueOf(authProviderType.name());
  }
}
