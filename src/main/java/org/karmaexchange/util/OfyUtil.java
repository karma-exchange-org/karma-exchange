package org.karmaexchange.util;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public class OfyUtil {

  public static <T> Key<T> createKey(String str) {
    try {
      return Key.<T>create(str);
    } catch (IllegalArgumentException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    }
  }
}
