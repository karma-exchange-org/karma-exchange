package org.karmaexchange.bootstrap;

import static java.util.Arrays.asList;
import static org.karmaexchange.bootstrap.TestResourcesBootstrapTask.TestUser.*;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.Getter;

import org.apache.commons.lang3.time.DateUtils;
import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.EventParticipant;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import com.google.appengine.api.datastore.GeoPt;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

public class TestResourcesBootstrapTask extends BootstrapTask {

  private static int eventNum = 0;

  public enum TestUser {
    USER1("100006074376957", "Susan", "Liangberg"),
    USER2("100006058506752", "John", "Occhinostein"),
    USER3("100006051787601", "Rick", "Narayananson"),
    USER4("100006076592978", "Joe", "Warmanescu"),
    USER5("100006052237443", "Joe", "Narayananwitz"),
    USER6("100006093303024", "Ruth", "Carrierostein"),
    USER7("100006045731576", "Mary", "Greenestein"),
    USER8("100006080162988", "Richard", "Dinglewitz"),
    USER9("100006054577389", "Dick", "McDonaldberg"),
    USER10("100006053377578", "Linda", "Laverdetberg"),
    USER11("100006084302920", "Carol", "Wisemanwitz"),
    USER12("100006069696646", "Donna", "Zuckerson"),
    USER13("100006083973038", "Harry", "Occhinoman"),
    AMIR("1111368160", "Amir", "Valiani"),
    HARISH("537854733", "Harish", "Balijepalli");

    private final String fbId;
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;

    private TestUser(String fbId, String firstName, String lastName) {
      this.fbId = fbId;
      this.firstName = firstName;
      this.lastName = lastName;
    }

    public Key<User> getKey() {
      return User.getKey(createOAuthCredential());
    }

    public User createUser() {
      User user = User.create(createOAuthCredential());
      user.setFirstName(firstName);
      user.setLastName(lastName);
      return user;
    }

    private OAuthCredential createOAuthCredential() {
      return OAuthCredential.create(getSocialNetworkProviderType().toString(), fbId,
        "invalid_token");
    }

    public SocialNetworkProviderType getSocialNetworkProviderType() {
      return SocialNetworkProviderType.FACEBOOK;
    }
  }

  public TestResourcesBootstrapTask(PrintWriter statusWriter) {
    super(statusWriter);
  }

  @Override
  protected void performTask() {
    statusWriter.println("About to persist test users...");
    for (TestUser testUser : TestUser.values()) {
      User user = BaseDao.load(testUser.getKey());
      if (user == null) {
        BaseDao.upsert(testUser.createUser());
        User.updateProfileImage(testUser.getKey(), testUser.getSocialNetworkProviderType());
      }
    }
    statusWriter.println("About to persist test events...");
    for (Event event : createEvents()) {
      BaseDao.upsert(event);
    }
    statusWriter.println("Test users and events persisted.");
  }

  private static Collection<Event> createEvents() {
    List<Event> events = Lists.newArrayList();
    Date now = DateUtils.round(new Date(), Calendar.HOUR_OF_DAY);

    // Upcoming events.

    List<Key<User>> organizers = asList(USER1.getKey(), AMIR.getKey(), HARISH.getKey());
    List<Key<User>> registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey(), USER9.getKey(), USER10.getKey(),
      USER11.getKey(), USER12.getKey(), USER13.getKey());
    List<Key<User>> waitListedUsers = asList();
    events.add(createEvent("Amir & Harish Organizer - SF Street Cleanup",
      DateUtils.addDays(now, 1), 1, organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER2.getKey());
    registeredUsers = asList(USER1.getKey(), USER4.getKey());
    waitListedUsers = asList(USER3.getKey(), USER5.getKey());
    events.add(createEvent("Full event - Learning center",
      DateUtils.addDays(now, 3), 3, organizers, registeredUsers, waitListedUsers, 0, 2, 100));

    organizers = asList(AMIR.getKey());
    registeredUsers = asList();
    waitListedUsers = asList();
    events.add(createEvent("Amir Organizer - Date conflict - No one signed up",
      DateUtils.addDays(now, 12), 1, organizers, registeredUsers, waitListedUsers, 0, 5, 100));

    organizers = asList(HARISH.getKey());
    registeredUsers = asList();
    waitListedUsers = asList();
    events.add(createEvent("Harish Organizer - Date conflict - No one signed up",
      DateUtils.addDays(now, 12), 1, organizers, registeredUsers, waitListedUsers, 0, 5, 100));

    // Past events.

    organizers = asList(USER1.getKey(), HARISH.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey(), USER9.getKey(), USER10.getKey(),
      USER11.getKey(), USER12.getKey(), USER13.getKey(), AMIR.getKey());
    waitListedUsers = asList();
    events.add(createEvent("Harish as Organizer, Amir participant - SF Street Cleanup",
      DateUtils.addDays(now, -6), 1,
      organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER1.getKey(), AMIR.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey(), HARISH.getKey());
    waitListedUsers = asList();
    events.add(createEvent("Amir as Organizer, Harish participant - SF Street Cleanup",
      DateUtils.addDays(now, -13), 1,
      organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER1.getKey(), HARISH.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey());
    waitListedUsers = asList();
    events.add(createEvent("Harish as Organizer - SF Street Cleanup",
      DateUtils.addDays(now, -20), 1, organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER1.getKey(), AMIR.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey());
    waitListedUsers = asList();
    events.add(createEvent("Amir as Organizer - SF Street Cleanup",
      DateUtils.addDays(now, -27), 1, organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    return events;
  }

  private static Event createEvent(String title, Date startTime, int numHours,
      List<Key<User>> organizers, List<Key<User>> registeredUsers,
      List<Key<User>> waitListedUsers, int minRegistrations, int maxRegistrations,
      int maxWaitingList) {
    eventNum++;
    Event event = new Event();
    event.setTitle(title);
    if (eventNum % 2 == 0) {
      event.setDescription("The Eastmont Garden of Hope receives and distributes donated clothing, hygiene products, household items, canned goods, and other necessities to Social Services Agency clients in urgent need year-round. This program serves as many as 400 households monthly. Whether providing food for a struggling family, a warm coat for a shivering child, or diapers for a desperate mother’s newborn, the Eastmont Garden of Hope seeks to ensure that those in greatest need are served expeditiously and with utmost compassion. Volunteers play a vital role in the program's operations by managing donations and keeping the service area open and available to customers in need throughout the week.\n\n" +
          "Volunteers are requested to participate for at least one shift (3-4 hours) per week, for at least 3 months. Scheduling is flexible, Monday-Friday, between the hours of 9:00am-12:00pm and 1:00-5:00pm.");
      event.setSpecialInstructions("A mandatory Orientation is scheduled for Saturday, June 29, 2013 from 10:00-12:00 at the San Lorenzo Public Library (community meeting room): 395 Paseo Grande, San Lorenzo. Please contact Axxxx Wxxx, coordinator at XXX-XXX-XXXX or xxx@xxx.org to inquire about availability of this position and confirm attendance at orientation.");
      event.setCauses(KeyWrapper.create(
        asList(CauseType.getKey("Disabled"))));
    } else {
      event.setDescription("We are seeking volunteers willing to provide in-home respite care for people with terminally ill companion animals. Ancillary services would involve providing some comfort care for the pets themselves as well as pet loss counseling for those who have lost a pet. Risk management and liability issues are still being explored vis-a-vis a possible partnership with our local Humane Society of the North Bay, but we hope to begin training sessions for volunteers in 2014. In the meantime, volunteers will undergo initial screening through The NHFP. Please be patient if you do not hear from us right away. We are generally inundated with requests for emergency pet hospice care as well as involved with training seminars or our biennial symposium. We would appreciate a telephone call from you if you have not heard back from us within four weeks. Please call (XXX) XXX-XXXX and/or leave a message explaining you are a potential volunteer.");
      event.setCauses(KeyWrapper.create(
        asList(CauseType.getKey("Animals"))));
    }
    event.setLocation(createLocation());
    event.setStartTime(startTime);
    event.setEndTime(DateUtils.addHours(startTime, numHours));
    event.setParticipants(createParticpantsList(organizers, registeredUsers, waitListedUsers));
    event.setMinRegistrations(minRegistrations);
    event.setMaxRegistrations(maxRegistrations);
    event.setMaxWaitingList(maxWaitingList);
    return event;
  }

  private static Location createLocation() {
    Location location = new Location();
    location.setTitle("Ferry building");
    location.setDescription("The San Francisco Ferry Building is a terminal for ferries that " +
      "travel across the San Francisco Bay, a marketplace, and also has offices");
    Address address = new Address();
    location.setAddress(address);
    address.setStreet("1 Sausalito - San Francisco Ferry Bldg");
    address.setCity("San Francisco");
    address.setState("CA");
    address.setCountry("USA");
    address.setZip("94111");
    GeoPt geoPt = new GeoPt(37.7955f, -122.3937f);
    address.setGeoPt(GeoPtWrapper.create(geoPt));
    return location;
  }

  private static List<EventParticipant> createParticpantsList(List<Key<User>> organizers,
      List<Key<User>> registeredUsers, List<Key<User>> waitListedUsers) {
    List<EventParticipant> participants = Lists.newArrayList();
    for (Key<User> organizer : organizers) {
      participants.add(EventParticipant.create(organizer, ParticipantType.ORGANIZER));
    }
    for (Key<User> registeredUser : registeredUsers) {
      participants.add(EventParticipant.create(registeredUser, ParticipantType.REGISTERED));
    }
    for (Key<User> waitListedUser : waitListedUsers) {
      participants.add(EventParticipant.create(waitListedUser, ParticipantType.WAIT_LISTED));
    }
    return participants;
  }
}
