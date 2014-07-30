package org.karmaexchange.dao;

import org.karmaexchange.dao.Organization.SourceOrganizationInfo;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrganizationPrefs {

  private KeyWrapper<Organization> org;
  // This is done as an optimization to avoid looking up the listing org each time.
  private SourceOrganizationInfo sourceOrgInfo;
  private boolean emailOptOut;

  public OrganizationPrefs(Key<Organization> orgKey, SourceOrganizationInfo sourceOrgInfo,
      boolean emailOptOut) {
    org = KeyWrapper.create(orgKey);
    this.sourceOrgInfo = sourceOrgInfo;
    this.emailOptOut = emailOptOut;
  }
}
