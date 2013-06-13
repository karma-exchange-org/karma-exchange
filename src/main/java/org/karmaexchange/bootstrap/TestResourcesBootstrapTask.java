package org.karmaexchange.bootstrap;

import static java.util.Arrays.asList;
import static org.karmaexchange.bootstrap.TestResourcesBootstrapTask.TestUser.*;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

  @RequiredArgsConstructor
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
    USER13("100006083973038", "Harry", "Occhinoman");

    private final String fbId;
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;

    public Key<User> getKey() {
      return User.createKey(createOAuthCredential());
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
    Date now = new Date();
    now = DateUtils.round(now, Calendar.HOUR_OF_DAY);

    // Upcoming events.

    List<Key<User>> organizers = asList(USER1.getKey());
    List<Key<User>> registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey(), USER9.getKey(), USER10.getKey(),
      USER11.getKey(), USER12.getKey(), USER13.getKey());
    List<Key<User>> waitListedUsers = asList();
    events.add(createEvent("SF Street Cleanup", DateUtils.addDays(now, 1), 1,
      organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER2.getKey());
    registeredUsers = asList(USER1.getKey(), USER4.getKey());
    waitListedUsers = asList(USER3.getKey(), USER5.getKey());
    events.add(createEvent("First Graduate invites you to be a Presentations of Learning Evaluator",
      DateUtils.addDays(now, 3), 3, organizers, registeredUsers, waitListedUsers, 0, 2, 100));

    organizers = asList(USER4.getKey());
    registeredUsers = asList();
    waitListedUsers = asList();
    events.add(createEvent("Inner City Youth Community Outreach",
      DateUtils.addDays(now, 12), 1, organizers, registeredUsers, waitListedUsers, 0, 5, 100));

    // Past events.

    organizers = asList(USER1.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey(), USER9.getKey(), USER10.getKey(),
      USER11.getKey(), USER12.getKey(), USER13.getKey());
    waitListedUsers = asList();
    events.add(createEvent("SF Street Cleanup", DateUtils.addDays(now, -6), 1,
      organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    organizers = asList(USER1.getKey());
    registeredUsers = asList(USER2.getKey(), USER4.getKey(), USER5.getKey(),
      USER6.getKey(), USER7.getKey(), USER8.getKey());
    waitListedUsers = asList();
    events.add(createEvent("SF Street Cleanup", DateUtils.addDays(now, -13), 1,
      organizers, registeredUsers, waitListedUsers, 0, 100, 100));

    return events;
  }

  private static Event createEvent(String title, Date startTime, int numHours,
      List<Key<User>> organizers, List<Key<User>> registeredUsers,
      List<Key<User>> waitListedUsers, int minRegistrations, int maxRegistrations,
      int maxWaitingList) {
    Event event = new Event();
    event.setTitle(title);
    event.setDescription(repeat("Repeating description.", 500));
    event.setSpecialInstructions(repeat("Repeating special instructions.", 240));
    event.setCauses(KeyWrapper.create(
      asList(CauseType.createKey("Animals"), CauseType.createKey("Disabled"))));
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

  private static String repeat(String string, int numChars) {
    StringBuffer result = new StringBuffer(string);
    while (result.length() < numChars) {
      result.append(" ");
      result.append(string);
    }
    return result.toString();
  }
}
