package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;

@Embed
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class OrganizationNamedKeyWrapper extends NamedKeyWrapper<Organization> {

  public OrganizationNamedKeyWrapper(Key<Organization> orgKey) {
    super(orgKey);
  }

  public OrganizationNamedKeyWrapper(Organization org) {
    super(Key.create(org), org.getOrgName());
  }

  public void updateName() {
    Organization org = ofy().transactionless().load().key(key).now();
    if (org == null) {
      throw new IllegalArgumentException("org does not exist: " + key.getString());
    }
    name = org.getOrgName();
  }
}
