package org.karmaexchange.dao;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ EventTest.class, OrganizationTest.class, RatingTest.class, UserTest.class})
public class AllTests {

}
