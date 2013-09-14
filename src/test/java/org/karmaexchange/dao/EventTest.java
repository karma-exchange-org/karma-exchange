package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;
import static org.karmaexchange.util.TestUtil.DEBUG;

import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.dao.Event.CompletionTaskTracker;
import org.karmaexchange.dao.Event.CompletionTaskTracker.CompletionTaskWrapper;
import org.karmaexchange.dao.Event.CompletionTaskTracker.OrganizationCompletionTask;
import org.karmaexchange.dao.Event.CompletionTaskTracker.ParticipantCompletionTask;
import org.karmaexchange.util.DatastoreTestUtil;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

public class EventTest extends PersistenceTestHelper {

  private Event event;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    event = new Event();
    event.setId(Long.valueOf(25));
    event.setTitle("title");

    event.setCauses(asList(CauseType.HOMELESSNESS));

    Location location = new Location();
    event.setLocation(location);

    event.setRegistrationInfo(Event.RegistrationInfo.CAN_REGISTER);
    event.setMaxRegistrations(20);
    event.setKarmaPoints(100);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));

    // Validate that we don't hit the objectify bug of embedded lists.
    CompletionTaskTracker completionTasks = new CompletionTaskTracker();
    User fakeUser = new User();
    fakeUser.setName("fakeUser");
    Key<User> fakeUserKey = Key.create(fakeUser);
    Organization fakeOrg = new Organization();
    fakeOrg.setName("fakeOrg");
    Key<Organization> fakeOrgKey = Key.create(fakeOrg);
    List<CompletionTaskWrapper> tasksPending = Lists.newArrayList();
    ParticipantCompletionTask participantTask1 = new ParticipantCompletionTask(fakeUserKey);
    OrganizationCompletionTask orgCompletionTask1 = new OrganizationCompletionTask(fakeOrgKey);
    ParticipantCompletionTask participantTask2 = new ParticipantCompletionTask(fakeUserKey);
    tasksPending.add(new CompletionTaskWrapper(participantTask1));
    tasksPending.add(new CompletionTaskWrapper(orgCompletionTask1));
    tasksPending.add(new CompletionTaskWrapper(participantTask2));
    completionTasks.setTasksPending(tasksPending);
    event.setCompletionTasks(completionTasks);
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion(event, Event.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(event);
    if (DEBUG) {
      DatastoreTestUtil.dumpEntity(event);
    }
  }
}
