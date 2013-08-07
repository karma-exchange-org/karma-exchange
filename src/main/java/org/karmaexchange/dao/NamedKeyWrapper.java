package org.karmaexchange.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public abstract class NamedKeyWrapper<T> extends KeyWrapper<T> {

  protected String name;

  protected NamedKeyWrapper(Key<T> key) {
    super(key);
    updateName();
  }

  protected NamedKeyWrapper(Key<T> key, String name) {
    super(key);
    this.name = name;
  }

  public abstract void updateName();
}
