package org.karmaexchange.dao.derived;

import lombok.Data;

/*
 * This class provides an identical structure to KeyWrapper however it doesn't
 * force translate the key into a datastore key.
 */
@Data
public class SourceKeyWrapper {
  private String key;


}
