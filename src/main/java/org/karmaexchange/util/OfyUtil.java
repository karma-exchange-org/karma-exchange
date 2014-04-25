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

  public static String getKind(Class<?> cls) {
    return ofy().factory().getMetadata(cls).getKeyMetadata().getKind();
  }
}
