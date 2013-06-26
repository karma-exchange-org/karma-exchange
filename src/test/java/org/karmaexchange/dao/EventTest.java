package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;
import static org.karmaexchange.util.TestUtil.debug;

import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.util.DatastoreTestUtil;

import com.google.common.collect.Lists;

public class EventTest extends PersistenceTestHelper {

  private Event event;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    event = new Event();
    event.setId(Long.valueOf(25));
    event.setTitle("title");

    event.setCauses(asList(
      KeyWrapper.create(CauseType.create("homeless"))));

    Location location = new Location();
    event.setLocation(location);

    event.setRegistrationInfo(Event.RegistrationInfo.CAN_REGISTER);
    event.setMaxRegistrations(20);
    event.setKarmaPoints(100);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion(event, Event.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(event);
    if (debug) {
      DatastoreTestUtil.dumpEntity(event);
    }
  }
}
