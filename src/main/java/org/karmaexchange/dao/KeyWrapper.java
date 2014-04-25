package org.karmaexchange.dao;

import java.util.Collection;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

/**
 * This class wraps Objectify keys to enable keys to be converted by JAXB.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
@Data
@NoArgsConstructor
public class KeyWrapper<T> implements Comparable<KeyWrapper<T>> {

  protected Key<T> key;

  public static <T> KeyWrapper<T> create(T obj) {
    return create(Key.create(obj));
  }

  public static <T> KeyWrapper<T> create(Key<T> key) {
    return new KeyWrapper<T>(key);
  }

  protected KeyWrapper(Key<T> key) {
    this.key = key;
  }

  public static <T> List<KeyWrapper<T>> create(Collection<Key<T>> keys) {
    List<KeyWrapper<T>> wrappedKeys = Lists.newArrayList();
    for (Key<T> key : keys) {
      wrappedKeys.add(create(key));
    }
    return wrappedKeys;
  }

  public void setKey(String keyStr) {
    key = Key.<T>create(keyStr);
  }

  public String getKey() {
    return key.getString();
  }

  /*
  public Key<T> getKeyObj() {
    return key;
  }
  */

  public static <T> Key<T> toKey(KeyWrapper<T> wrapper) {
    return wrapper.key;
  }

  public static <T> List<Key<T>> toKeys(List<KeyWrapper<T>> wrappedKeys) {
    List<Key<T>> keys = Lists.newArrayListWithCapacity(wrappedKeys.size());
    for (KeyWrapper<T> wrappedKey : wrappedKeys) {
      keys.add(wrappedKey.key);
    }
    return keys;
  }

  @Override
  public int compareTo(KeyWrapper<T> other) {
    return this.key.compareTo(other.key);
  }
}
