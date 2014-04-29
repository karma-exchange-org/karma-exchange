package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;

import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

public class OrganizationTest extends PersistenceTestHelper {

  Organization org;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    org = new Organization();
    org.setName("org");
    org.setOrgName("My Org");
    org.setPage(PageRef.create("org", "http://facebook.com/org",
      SocialNetworkProviderType.FACEBOOK));
    // org.setType(Organization.Type.NON_PROFIT);
    org.setCauses(asList(CauseType.HOMELESSNESS, CauseType.MENTORSHIP));

    User u1 = new User();
    u1.setId(Long.valueOf(101));
    User u2 = new User();
    u2.setId(Long.valueOf(101));
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion((Organization) org, Organization.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(org);
    assertTrue(org instanceof Organization);
  }
}
