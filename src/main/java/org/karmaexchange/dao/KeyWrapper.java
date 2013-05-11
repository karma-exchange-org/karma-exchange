package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import lombok.Data;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;

/**
 * This class wraps Objectify keys to enable keys to be converted by JAXB.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
@Data
@Embed
public final class KeyWrapper<T> implements Comparable<KeyWrapper<T>>{

  private Key<T> key;

  public static <T> KeyWrapper<T> create(T obj) {
    return create(Key.create(obj));
  }

  public static <T> KeyWrapper<T> create(Key<T> key) {
    KeyWrapper<T> wrapper = new KeyWrapper<T>();
    wrapper.key = key;
    return wrapper;
  }

  public void setKey(String keyStr) {
    key = Key.<T>create(keyStr);
  }

  public String getKey() {
    return key.getString();
  }

  public static <T> List<Key<T>> getKeyObjs(List<KeyWrapper<T>> wrappedKeys) {
    List<Key<T>> keys = Lists.newArrayListWithCapacity(wrappedKeys.size());
    for (KeyWrapper<T> wrappedKey : wrappedKeys) {
      keys.add(wrappedKey.key);
    }
    return keys;
  }

  public T fetchEntity() {
    return ofy().load().key(key).now();

  }

  @Override
  public int compareTo(KeyWrapper<T> other) {
    return this.key.compareTo(other.key);
  }
}
