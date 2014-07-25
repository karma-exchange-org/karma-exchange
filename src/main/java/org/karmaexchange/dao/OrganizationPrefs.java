package org.karmaexchange.dao;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrganizationPrefs {

  private KeyWrapper<Organization> org;
  private boolean emailOptOut;

  public OrganizationPrefs(Key<Organization> orgKey, boolean emailOptOut) {
    org = KeyWrapper.create(orgKey);
    this.emailOptOut = emailOptOut;
  }
}
