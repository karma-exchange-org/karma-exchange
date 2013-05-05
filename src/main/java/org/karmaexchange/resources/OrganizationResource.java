package org.karmaexchange.resources;

import javax.ws.rs.Path;

import org.karmaexchange.dao.Organization;

@Path("/organization")
public class OrganizationResource extends BaseResource<Organization> {

  @Override
  protected Class<Organization> getResourceClass() {
    return Organization.class;
  }

  @Override
  protected long getResourceId(Organization organization) {
    return organization.getId();
  }
}
