package org.karmaexchange.resources.derived;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.OrganizationPrefs;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.derived.EventSourceInfo;

import com.google.api.client.util.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class SourceUser extends BaseSourceUser {

  private boolean emailOptOut;
  private List<SourceOrganizationPrefs> orgPrefs;

  public void upsert(EventSourceInfo listingOrgInfo) {
    validate();

    Map<Key<Organization>, OrganizationPrefs> orgPrefsMap =
        toOrganizationPrefsMap(listingOrgInfo);

    GlobalUidMapping mapping =
        ofy().load().key(getGlobalUidMappingKey()).now();
    if (mapping == null) {
      UserInfo userInfo =
          createUser(listingOrgInfo.getOrgKey());
      User user =
          userInfo.getUser();
      user.setEmailOptOut(emailOptOut);
      user.setOrgPrefs(new ArrayList<>(orgPrefsMap.values()));
      User.upsertNewUser(userInfo);
    } else {
      // We ignore the global emailOptOut field for existing users since the same
      // user can be registered with multiple orgs. Therefore only update the per org settings.
      ofy().transact(
        new UpdateUserPrefsTxn(mapping.getUserKey(), orgPrefsMap));
    }
  }

  private Map<Key<Organization>, OrganizationPrefs> toOrganizationPrefsMap(
      EventSourceInfo listingOrgInfo) {
    Map<Key<Organization>, OrganizationPrefs> orgPrefsMap =
        Maps.newHashMap();

    for (SourceOrganizationPrefs sourceOrgPrefs : orgPrefs) {
      OrganizationPrefs convOrgPrefs =
          sourceOrgPrefs.toOrganizationPrefs(listingOrgInfo);
      orgPrefsMap.put(
        KeyWrapper.toKey(convOrgPrefs.getOrg()),
        convOrgPrefs);
    }

    return orgPrefsMap;
  }


  private void validate() {
    for (SourceOrganizationPrefs orgPref : orgPrefs) {
      orgPref.validate();
    }
  }

  @Data
  @NoArgsConstructor
  public static class SourceOrganizationPrefs {
    private SourceOrganization org;
    private boolean emailOptOut;

    public void validate() {
      org.validate();
    }

    public OrganizationPrefs toOrganizationPrefs(EventSourceInfo listingOrgInfo) {
      Key<Organization> orgKey =
          org.lookupOrCreateOrg(listingOrgInfo);
      return new OrganizationPrefs(orgKey, emailOptOut);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  private static class UpdateUserPrefsTxn extends VoidWork {
    private final Key<User> userKey;
    private final Map<Key<Organization>, OrganizationPrefs> updatedOrgPrefsMap;

    public void vrun() {
      User user = ofy().load().key(userKey).now();
      if (user == null) {
        // User has been deleted.
        return;
      }

      boolean updateRequired = false;

      Map<Key<Organization>, OrganizationPrefs> existingOrgPrefsMap =
          toOrgPrefsMap(user);
      for (Map.Entry<Key<Organization>, OrganizationPrefs> updatedOrgPrefEntry :
            updatedOrgPrefsMap.entrySet()) {
        OrganizationPrefs existingPrefs =
            existingOrgPrefsMap.get(updatedOrgPrefEntry.getKey());
        OrganizationPrefs newPrefs =
            updatedOrgPrefEntry.getValue();

        // Ignore identical updates.
        if ((existingPrefs != null) && existingPrefs.equals(newPrefs)) {
          continue;
        }

        // Insert / update required.
        existingOrgPrefsMap.put(
          updatedOrgPrefEntry.getKey(),
          updatedOrgPrefEntry.getValue());
        updateRequired = true;
      }

      if (updateRequired) {
        user.setOrgPrefs(new ArrayList<>(existingOrgPrefsMap.values()));
        BaseDao.partialUpdate(user);
      }
    }

    private Map<Key<Organization>, OrganizationPrefs> toOrgPrefsMap(User user) {
      Map<Key<Organization>, OrganizationPrefs> orgPrefsMap =
          Maps.newHashMap();
      for (OrganizationPrefs orgPrefsEl : user.getOrgPrefs()) {
        orgPrefsMap.put(
          KeyWrapper.toKey(orgPrefsEl.getOrg()),
          orgPrefsEl);
      }
      return orgPrefsMap;
    }

  }
}
