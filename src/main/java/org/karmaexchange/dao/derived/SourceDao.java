package org.karmaexchange.dao.derived;

import org.karmaexchange.dao.Organization;

import lombok.Data;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Data
public class SourceDao {
  /*
   * This object is never persisted. It's only used to generate a unique mapping key.
   */

  @Id
  private String name;

  public static Key<SourceDao> createKey(Key<Organization> organizationKey,
      String sourceKey) {
    return Key.<SourceDao>create(SourceDao.class,
      Organization.getUniqueOrgId(organizationKey) + ":" + sourceKey);
  }
}
