package org.karmaexchange.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalUid {
  private static String TYPE_ID_SEPERATOR = ":";

  private String id;

  public static GlobalUid create(AuthProviderType providerType, String id) {
    return new GlobalUid(providerType.name() + TYPE_ID_SEPERATOR + id);
  }
}
