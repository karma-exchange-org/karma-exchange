package org.karmaexchange.util;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public class OfyUtil {

  public static <T> Key<T> createKey(String str) {
    try {
      return Key.<T>create(str);
    } catch (IllegalArgumentException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    } catch (NullPointerException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    }
  }

  /**
   * Creates a Key object if the input is a valid key.
   *
   * @param keyStr the encoded key to parse
   * @return the Key object for the input keyStr
   * @throws IllegalArgumentException if the string is non-null and not a valid key.
   */
  public static <T> Key<T> createIfKey(String keyStr) {
    try {
      return Key.<T>create(keyStr);
    } catch (NullPointerException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    }
  }

  public static String getKind(Class<?> cls) {
    return ofy().factory().getMetadata(cls).getKeyMetadata().getKind();
  }
}
