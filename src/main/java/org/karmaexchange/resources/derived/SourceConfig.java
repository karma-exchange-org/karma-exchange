package org.karmaexchange.resources.derived;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Organization.SourceOrganizationInfo;
import org.karmaexchange.dao.derived.EventSourceInfo;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceConfig {

  private String rootOrgId;

  public void upsert(EventSourceInfo sourceInfo) {
    Key<Organization> orgKey = sourceInfo.getOrgKey();
    Organization org =
        ofy().load().key(orgKey).now();
    SourceOrganizationInfo persistedOrgInfo =
        org.getSourceOrgInfo();

    if (persistedOrgInfo == null) {
      persistedOrgInfo = new SourceOrganizationInfo();
    }

    boolean updateRequired = false;
    if ((persistedOrgInfo.getId() == null) ||
        !persistedOrgInfo.getId().equals(rootOrgId)) {
      persistedOrgInfo.setId(rootOrgId);
      updateRequired = true;
    }
    if ((persistedOrgInfo.getListingOrg() == null) ||
        !persistedOrgInfo.getListingOrg().equals(orgKey)) {
      persistedOrgInfo.setListingOrg(
        KeyWrapper.create(orgKey));
      updateRequired = true;
    }
    if (updateRequired) {
      ofy().transact(
        new UpdateOrgConfigTxn(orgKey, persistedOrgInfo));
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class UpdateOrgConfigTxn extends VoidWork {

    private final Key<Organization> orgKey;
    private final SourceOrganizationInfo updatedSourceOrgInfo;

    public void vrun() {
      Organization org =
          ofy().load().key(orgKey).now();

      if (org == null) {
        // Org has been deleted
        return;
      }

      org.setSourceOrgInfo(updatedSourceOrgInfo);
      BaseDao.partialUpdate(org);
    }

  }

}