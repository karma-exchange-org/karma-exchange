package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;

import org.junit.Before;
import org.junit.Test;

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
      KeyWrapper.create(Cause.create("homeless"))));

    Location location = new Location();
    event.setLocation(location);

    event.setRegistrationInfo(Event.RegistrationInfo.CAN_REGISTER);
    event.setMaxRegistrations(20);
    event.setKarmaPoints(100);
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion(event, Event.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(event);
  }
}
