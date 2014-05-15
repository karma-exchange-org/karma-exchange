package org.karmaexchange.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GlobalUid {
  private static String TYPE_ID_SEPERATOR = ":";

  private String id;

  public GlobalUid(GlobalUidType type, String id) {
    if (type == GlobalUidType.EMAIL) {
      id = id.toLowerCase();
    }
    this.id = type.name() + TYPE_ID_SEPERATOR + id;
  }
}
