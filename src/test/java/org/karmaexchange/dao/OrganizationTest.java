package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;

import org.junit.Before;
import org.junit.Test;

public class OrganizationTest extends PersistenceTestHelper {

  Organization org;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    org = new Organization();
    org.setId(Long.valueOf(25));
    // org.setType(Organization.Type.NON_PROFIT);
    org.setCauses(asList(
      KeyWrapper.create(CauseType.create("homeless")),
      KeyWrapper.create(CauseType.create("marriage equality"))));

    User u1 = new User();
    u1.setName("fake name1");
    User u2 = new User();
    u2.setName("fake name2");
    org.setAdmins(asList(
      KeyWrapper.create(u1),
      KeyWrapper.create(u2)));
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
