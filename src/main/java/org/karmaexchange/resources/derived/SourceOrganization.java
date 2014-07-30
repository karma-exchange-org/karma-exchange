package org.karmaexchange.resources.derived;

import static org.karmaexchange.util.OfyService.ofy;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.dao.Organization.SourceOrganizationInfo;
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.provider.FacebookSocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

@Data
@NoArgsConstructor
public final class SourceOrganization {

  private String id;
  private String name;

  @Nullable
  private String orgId;
  @Nullable
  private String secretKey;

  // This constructor is only meant to be used for source update requests.
  public SourceOrganization(SourceOrganizationInfo sourceOrgInfo) {
    id = sourceOrgInfo.getId();
  }

  public void validate() {
    if ((id == null) || (name == null)) {
      throw ErrorResponseMsg.createException("invalid org info: " + this,
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  public Key<Organization> lookupOrCreateOrg(EventSourceInfo listingOrgInfo) {
    Organization listingOrg =
        ofy().load().key(listingOrgInfo.getOrgKey()).now();
    SourceOrganizationInfo listingOrgIdInfo =
        listingOrg.getSourceOrgInfo();

    // Check if the org being looked up is the listing org.
    if (id.equals(listingOrgIdInfo.getId())) {
      // Use the saved org name and not the one specified in the remote db.
      name = listingOrg.getOrgName();
      orgId = Organization.getOrgId(listingOrgInfo.getOrgKey());
      return listingOrgInfo.getOrgKey();
    }

    Key<Organization> orgKey = null;
    Organization org;
    if (orgId == null) {
      orgId =
          computeOrgId(listingOrgInfo);
      orgKey =
          Organization.createKey(orgId);
      org =
          ofy().load().key(orgKey).now();
      if (org == null) {
        // We automatically create organizations for organizations that
        // do not have an existing org.
        org = new Organization();
        org.setName(
          Organization.orgIdToName(orgId));
        org.setOrgName(name);
        org.setSourceOrgInfo(
          new SourceOrganizationInfo(id, Key.create(listingOrg)));
        org.setListingOrgPage(listingOrg.getPage());

        SourceOrganization.CreateOrganizationTxn createOrgTxn =
            new CreateOrganizationTxn(org);
        ofy().transact(createOrgTxn);
        org = createOrgTxn.org;
      }
    } else {
      // TESTING: We're disabling the org secret check for now.
      // EventSourceInfo.validateOrgSecret(orgId, secretKey);

      orgKey =
          Organization.createKey(orgId);
      org =
          ofy().load().key(orgKey).now();

      // TESTING: We're auto creating orgs that don't exist.
      if (org == null) {
        org = createOrganizationFromFb(orgId, listingOrgInfo.getOrgKey());
      }
    }

    // Use the saved org name and not the one specified in the remote db.
    name = org.getOrgName();

    return orgKey;
  }

  private String computeOrgId(EventSourceInfo listingOrgInfo) {
    String listingOrgId =
        Organization.getOrgId(listingOrgInfo.getOrgKey());
    String orgNameSuffix = name.replaceAll("\\s","").toLowerCase();
    return listingOrgId + "." + orgNameSuffix;
  }

  // TESTING
  private Organization createOrganizationFromFb(String orgId, Key<Organization> listingOrgKey) {
    Organization org = new Organization();
    org.setPage(PageRef.create(orgId, FacebookSocialNetworkProvider.PAGE_BASE_URL + orgId,
      SocialNetworkProviderType.FACEBOOK));
    org.initFromPage();
    org.setSourceOrgInfo(
      new SourceOrganizationInfo(id, listingOrgKey));
    SourceOrganization.CreateOrganizationTxn createOrgTxn =
        new CreateOrganizationTxn(org);
    ofy().transact(createOrgTxn);
    return createOrgTxn.org;
  }

  @Data
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper=false)
  private static class CreateOrganizationTxn extends VoidWork {

    private Organization org;

    public void vrun() {
      Key<Organization> orgKey =
          Key.create(org);
      Organization existingOrg =
          ofy().load().key(orgKey).now();
      if (existingOrg == null) {
        BaseDao.upsert(org);
      } else {
        org = existingOrg;
      }
    }
  }
}