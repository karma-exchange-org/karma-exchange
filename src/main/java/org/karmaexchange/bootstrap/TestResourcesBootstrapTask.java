package org.karmaexchange.bootstrap;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.karmaexchange.bootstrap.TestResourcesBootstrapTask.TestUser.*;
import static org.karmaexchange.bootstrap.TestResourcesBootstrapTask.TestOrganization.*;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.Properties.Property.FACEBOOK_APP_ID;
import static org.karmaexchange.util.Properties.Property.FACEBOOK_APP_SECRET;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.lang3.time.DateUtils;
import org.karmaexchange.auth.AuthProviderType;
import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.GlobalUidType;
import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.AlbumRef;
import org.karmaexchange.dao.Badge;
import org.karmaexchange.dao.BadgeSummary;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.EventParticipant;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.Event.UpsertParticipantTxn;
import org.karmaexchange.dao.Organization.AutoMembershipRule;
import org.karmaexchange.dao.OrganizationNamedKeyWrapper;
import org.karmaexchange.dao.User.KarmaGoal;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.ImageProviderType;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.SuitableForType;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserManagedEvent;
import org.karmaexchange.dao.Waiver;
import org.karmaexchange.provider.FacebookSocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.task.ComputeLeaderboardServlet;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.Properties;
import org.karmaexchange.util.AdminUtil.AdminSubtask;

import com.google.appengine.api.datastore.GeoPt;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.FacebookClient.AccessToken;

public class TestResourcesBootstrapTask extends BootstrapTask {

  private static int eventNum = 0;

  private Map<String, User> testUserMap = Maps.newHashMap();

  public enum TestUser {
    USER1("100006074376957", "Katie", "Hilary", "Katie.Hilary@KarmaExchange.org",
      Long.valueOf(6 * 60), false),
    USER2("100006058506752", "Alan", "Franjo", "alan.franjo@KidsClub.org"),
    USER3("100006051787601", "Kenneth", "Gilbert", "kenneth.gilbert@kidsclub.org"),
    USER4("100006076592978", "Eddie", "Kenelm", "Eddie@karmaexchange.org"),
    USER5("100006052237443", "Will", "Lorne", "Will@karmaexchange.org"),
    USER6("100006093303024", "Courtney", "Arden", "Courtney@karmaexchange.org"),
    USER7("100006045731576", "Leslie", "Lavern", "Leslie@karmaexchange.org"),
    USER8("100006080162988", "Darryl", "Teo", "Darryl@karmaexchange.org"),
    USER9("100006054577389", "Johnny", "Lev", "lev@fakekidsclub.org"),
    USER10("100006053377578", "Cassidy", "Sadie", "cas@karmaexchange.org"),
    USER11("100006084302920", "Michelle", "Dailey", "michelle.daileyl@kidsclub.org"),
    USER12("100006069696646", "Erica", "Wade", "erica@karmaexchange.org"),
    USER13("100006083973038", "Todd", "Angelos", "todd@karmaexchange.org"),
    AMIR("1111368160", "Amir", "Valiani", "amir@karmaexchange.org",
      Long.valueOf(8 * 60), true),
    HARISH("537854733", "Harish", "Balijepalli", "harish@karmaexchange.org",
      Long.valueOf(4 * 60), true),
    POONUM("3205292", "Poonum", "Kaberwal", "poonum@karmaexchange.org",
      Long.valueOf(4 * 60), true);

    @Getter
    private final String fbId;
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;
    @Getter
    private final String email;
    @Getter
    private final Long monthlyKarmaPtsGoal;
    @Getter
    private final boolean realUser;

    private TestUser(String fbId, String firstName, String lastName, @Nullable String email) {
      this(fbId, firstName, lastName, email, null, false);
    }

    private TestUser(String fbId, String firstName, String lastName, @Nullable String email,
        Long monthlyKarmaPtsGoal, boolean realUser) {
      this.fbId = fbId;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
      this.monthlyKarmaPtsGoal = monthlyKarmaPtsGoal;
      this.realUser = realUser;
    }

    public UserInfo createUser(Map<String, User> userMap) {
      User user = User.create();
      user.setFirstName(firstName);
      user.setLastName(lastName);
      if ((this != AMIR) && (this != HARISH) && (this != POONUM) &&
          ((Long.valueOf(fbId) % 2) != 0)) {
        user.setAbout("I'm looking forward to making a difference!");
      }
      user.getRegisteredEmails().add(new RegisteredEmail(email, true));
      if (monthlyKarmaPtsGoal != null) {
        KarmaGoal goal = new KarmaGoal();
        user.setKarmaGoal(goal);
        goal.setMonthlyGoal(monthlyKarmaPtsGoal);
      }

      // If the user is a non-test facebook user, use facebook to provide the up to date info.
      if (realUser) {
        initUser(fbId, user);
      }

      userMap.put(fbId, user);

      return new UserInfo(user,
        new UserInfo.ProfileImage(
          ImageProviderType.FACEBOOK,
          FacebookSocialNetworkProvider.getProfileImageUrl(fbId)) );
    }

    private static void initUser(String fbId, User user) {
      String appId = Properties.get(FACEBOOK_APP_ID);
      String appSecret = Properties.get(FACEBOOK_APP_SECRET);
      AccessToken accessToken =
          new DefaultFacebookClient().obtainAppAccessToken(appId, appSecret);
      DefaultFacebookClient fbClient = new DefaultFacebookClient(accessToken.getAccessToken());
      com.restfb.types.User fbUser = fbClient.fetchObject(fbId, com.restfb.types.User.class,
        Parameter.with("fields", "id, first_name, last_name, email"));

      user.setFirstName(fbUser.getFirstName());
      user.setLastName(fbUser.getLastName());
      if (fbUser.getEmail() != null) {
        user.setRegisteredEmails(Lists.newArrayList(new RegisteredEmail(fbUser.getEmail(), true)));
      }
    }

    public SocialNetworkProviderType getSocialNetworkProviderType() {
      return SocialNetworkProviderType.FACEBOOK;
    }
  }

  public enum TestOrganization {
    BGCSF("BGCSF",
      "https://bgcsf.thankyou4caring.org/page.aspx?pid=298",
      AMIR,
      asList(
        TestOrgMembership.of(USER7, Organization.Role.MEMBER, Organization.Role.ORGANIZER),
        TestOrgMembership.of(USER6, Organization.Role.MEMBER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER5, Organization.Role.ORGANIZER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER1, Organization.Role.MEMBER, null),
        TestOrgMembership.of(USER9, null, Organization.Role.MEMBER), // fake email, not granted
        TestOrgMembership.of(USER10, null, Organization.Role.ADMIN),
        TestOrgMembership.of(USER2, null, Organization.Role.MEMBER),  // auto-grant
        TestOrgMembership.of(USER11, null, Organization.Role.ORGANIZER),  // auto-grant
        TestOrgMembership.of(USER3, null, Organization.Role.ADMIN)), // not-auto-granted
      null,
      asList(new AutoMembershipRule("kidsclub.org", Organization.Role.ORGANIZER)),
      asList(createWaiver("Soccer clinic waiver",
                "If you are injured while volunteering BGCSF is not liable.\n\n" +
                "If your property is stolen while volunteering that is your responsibility."),
             createWaiver("After school tutoring waiver",
                "You are responsible for any property you bring."))),

    BGCSF_COLUMBIA_PARK("columbia.park",
      "https://bgcsf.thankyou4caring.org/page.aspx?pid=298",
      AMIR,
      asList(
        TestOrgMembership.of(USER1, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(USER8, null, Organization.Role.ORGANIZER),
        TestOrgMembership.of(HARISH, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(POONUM, Organization.Role.ORGANIZER, null)),
      BGCSF,
      ImmutableList.<AutoMembershipRule>of(),
      asList(createWaiver("Soccer clinic waiver",
        "If you are injured while volunteering Columbia Park BGCSF is not liable.\n\n" +
        "If your property is stolen while volunteering that is your responsibility."))),

    BGCSF_TENDERLOIN("Tenderloin.clubhouse",
      "https://bgcsf.thankyou4caring.org/page.aspx?pid=298",
      HARISH,
      asList(
        TestOrgMembership.of(POONUM, Organization.Role.ADMIN, null),
        TestOrgMembership.of(USER1, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(USER2, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(USER8, null, Organization.Role.ORGANIZER)),
      BGCSF),

    BENEVOLENT("benevolent.net",
      "https://www.benevolent.net/about/donate.html",
      HARISH,
      asList(
        TestOrgMembership.of(POONUM, Organization.Role.ADMIN, null),
        TestOrgMembership.of(USER7, Organization.Role.MEMBER, Organization.Role.ORGANIZER),
        TestOrgMembership.of(USER6, Organization.Role.MEMBER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER5, Organization.Role.ORGANIZER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER1, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(USER2, null, Organization.Role.MEMBER),
        TestOrgMembership.of(USER3, null, Organization.Role.ADMIN))),

    BFHP("BerkeleyFoodandHousingProject",
      "http://bfhp.org/donate/",
      POONUM,
      asList(
        TestOrgMembership.of(POONUM, Organization.Role.ADMIN, null))),

    STEWARDSHIP_NETWORK_NE("StewardshipNetworkNewEngland", null),

    UNITED_WAY("UnitedWay",
      "https://give.liveunited.org/page/contribute/support-us",
      AMIR,
      asList(
        TestOrgMembership.of(USER7, Organization.Role.MEMBER, Organization.Role.ORGANIZER),
        TestOrgMembership.of(USER6, Organization.Role.MEMBER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER5, Organization.Role.ORGANIZER, Organization.Role.ADMIN),
        TestOrgMembership.of(USER1, Organization.Role.ORGANIZER, null),
        TestOrgMembership.of(USER2, null, Organization.Role.MEMBER),
        TestOrgMembership.of(USER3, null, Organization.Role.ADMIN)));

    @Getter
    private final String pageName;
    @Getter
    private final String donationUrl;
    @Getter
    private final TestUser initialAdmin;
    @Getter
    private final List<TestOrgMembership> memberships;
    @Getter
    private final TestOrganization parentOrg;
    @Getter
    private final List<AutoMembershipRule> autoMembershipRules;
    @Getter
    private final List<Waiver> waivers;

    private TestOrganization(String pageName, String donationUrl) {
      this(pageName, donationUrl, null, ImmutableList.<TestOrgMembership>of());
    }

    private TestOrganization(String pageName, String donationUrl, TestUser initialAdmin,
        List<TestOrgMembership> memberships) {
      this(pageName, donationUrl, initialAdmin, memberships, null);
    }

    private TestOrganization(String pageName,  String donationUrl, TestUser initialAdmin,
        List<TestOrgMembership> memberships, @Nullable TestOrganization parentOrg) {
      this(pageName, donationUrl, initialAdmin, memberships, parentOrg,
        ImmutableList.<AutoMembershipRule>of(), ImmutableList.<Waiver>of());
    }

    private TestOrganization(String pageName, String donationUrl, TestUser initialAdmin,
        List<TestOrgMembership> memberships, @Nullable TestOrganization parentOrg,
        @Nullable List<AutoMembershipRule> autoMembershipRules,
        @Nullable List<Waiver> waivers) {
      this.pageName = pageName;
      this.donationUrl = donationUrl;
      this.initialAdmin = initialAdmin;
      this.memberships = memberships;
      this.parentOrg = parentOrg;
      this.autoMembershipRules = autoMembershipRules;
      this.waivers = waivers;
    }

    public PageRef getPageRef() {
      return PageRef.create(pageName, FacebookSocialNetworkProvider.PAGE_BASE_URL + pageName,
        SocialNetworkProviderType.FACEBOOK);
    }

    public Key<Organization> getKey() {
      return Key.create(Organization.class, Organization.orgIdToName(pageName));
    }

    private static Waiver createWaiver(String description, String content) {
      Waiver waiver = new Waiver();
      waiver.setDescription(description);
      waiver.setEmbeddedContent(content);
      return waiver;
    }
  }

  @Data(staticConstructor="of")
  private static class TestOrgMembership {
    private final TestUser user;
    private final Organization.Role grantedRole;
    private final Organization.Role requestedRole;
  }

  public TestResourcesBootstrapTask(PrintWriter statusWriter) {
    super(statusWriter);
  }

  @Override
  protected void performTask() {
    // Users must be persisted first to ensure that keys are available for each user.
    statusWriter.println("About to persist test users...");
    for (TestUser testUser : TestUser.values()) {
      GlobalUid globalUid =
          new GlobalUid(GlobalUidType.toGlobalUidType(AuthProviderType.FACEBOOK), testUser.getFbId());
      GlobalUidMapping userMapping = GlobalUidMapping.load(globalUid);

      if (userMapping != null) {
        throw new RuntimeException("DB has not been purged");
      }

      UserInfo userInfo = testUser.createUser(testUserMap);
      Key<User> persistedUserKey = User.upsertNewUser(userInfo);

      userMapping =
          new GlobalUidMapping(globalUid, persistedUserKey);
      ofy().save().entity(userMapping);  // Asynchronously save the new mapping
    }

    statusWriter.println("About to persist organizations...");
    persistOrganizations();
    statusWriter.println("About to persist test events...");
    Date now = new Date();
    CreateEventsResult createEventsResult = createEvents();
    for (Event event : createEventsResult.getEvents()) {
      BaseDao.upsert(event);
      // Process any completed events.
      if (event.getEndTime().before(now)) {
        Event.processEventCompletionTasks(Key.create(event));
      }
    }
    for (EventNoShowInfo noShowInfo : createEventsResult.eventNoShowInfo) {
      noShowInfo.persist();
      checkState(noShowInfo.event.getEndTime().before(now));
      Event.processEventCompletionTasks(Key.create(noShowInfo.event));
    }
    for (PendingReview pendingReview : createEventsResult.getPendingReviews()) {
      pendingReview.persistReview();
    }

    statusWriter.println("About to persist user managed test events...");
    persistUserManagedEvents();

    statusWriter.println("About to award badges...");
    awardBadges(createEventsResult.getEvents());

    statusWriter.println("About to update organization leaderboards...");
    String leaderboardJobId = ComputeLeaderboardServlet.startMapReduce();
    statusWriter.println("Leaderboard update initiated. View status at: " +
        ComputeLeaderboardServlet.getMapReduceStatusUrl("", leaderboardJobId));

    statusWriter.println("Test resources persisted.");
  }

  public Key<User> getKey(TestUser testUser) {
    User user = testUserMap.get(testUser.getFbId());
    checkNotNull(user);
    return Key.create(user);
  }

  private CreateEventsResult createEvents() {
    List<Event> events = Lists.newArrayList();
    Date now = new Date();

    Key<Waiver> bgcsfSoccerWaiverKey = Key.create(BGCSF.waivers.get(0));
    Key<Waiver> bgcsfTutoringWaiverKey = Key.create(BGCSF.waivers.get(1));
    Key<Waiver> columbiaParkSoccerWaiverKey = Key.create(BGCSF_COLUMBIA_PARK.waivers.get(0));

    Location ferryBuilding = new Location(
      "Ferry building",
      "The San Francisco Ferry Building is a terminal for ferries that " +
        "travel across the San Francisco Bay, a marketplace, and also has offices",
      new Address(
        "1 Sausalito - San Francisco Ferry Bldg",
        "San Francisco",
        "CA",
        "USA",
        "94111",
        GeoPtWrapper.create(new GeoPt(37.7955f, -122.3937f))));
    Location soccerField = new Location(
      "Youngblood-Coleman Playground",
      null,
      new Address(
        "1398 Hudson Avenue",
        "San Francisco",
        "CA",
        "USA",
        "94124",
        GeoPtWrapper.create(new GeoPt(37.738905f,-122.384654f))));
    Location bgcsfClubhouse = new Location(
      "BGCSF Clubhouse",
      null,
      new Address(
        "450 Guerrero St",
        "San Francisco",
        "CA",
        "USA",
        "94110",
        GeoPtWrapper.create(new GeoPt(37.763762f,-122.424233f))));
    Location unitedWayParkmoorOffice = new Location(
      "United Way Silicon Valley Office",
      null,
      new Address(
        "1400 Parkmoor Avenue #250",
        "San Jose",
        "CA",
        "USA",
        "95126",
        GeoPtWrapper.create(new GeoPt(37.316094f,-121.912575f))));
    Location benevolentParkmoorOffice = new Location(
      "Benevolent San Jose Office",
      null,
      new Address(
        "1400 Parkmoor Avenue",
        "San Jose",
        "CA",
        "USA",
        "95126",
        GeoPtWrapper.create(new GeoPt(37.316094f,-121.912575f))));
    Location bfhpMensShelter = new Location(
      "Men’s Overnight Shelter",
      null,
      new Address(
        "1931 Center Street",
        "Berkeley",
        "CA",
        "USA",
        "94704",
        GeoPtWrapper.create(new GeoPt( 37.8699523f, -122.27185150000003f ))));
    String bfhpCookAndServeDescription =
        "Meal Makers is a volunteer program that helps us provide meals in our shelters. Volunteers sign up to prepare a hot or cold meal off-site and deliver it to our shelters. Meal Makers can prepare a meal on a one-time or monthly basis.";

    List<Key<User>> organizers = asList(getKey(USER2));
    List<Key<User>> registeredUsers = asList(getKey(USER1), getKey(USER4));
    List<Key<User>> waitListedUsers = asList();
    Event event = createEvent("After School Tutoring", BGCSF_TENDERLOIN, bgcsfClubhouse,
      computeEventDate(now, 1, 3), 3, organizers, registeredUsers,
      waitListedUsers, 10, "502905379789560", bgcsfTutoringWaiverKey);
    event.setSuitableForTypes(Lists.newArrayList(SuitableForType.AGE_55_PLUS));
    events.add(event);

    organizers = asList(getKey(USER1), getKey(AMIR), getKey(HARISH),
      getKey(POONUM));
    registeredUsers = asList(getKey(USER2), getKey(USER4), getKey(USER5),
      getKey(USER6), getKey(USER7), getKey(USER8), getKey(USER9), getKey(USER10),
      getKey(USER11), getKey(USER12), getKey(USER13));
    waitListedUsers = asList();
    event = createEvent("Youth Soccer Clinic", BGCSF_COLUMBIA_PARK, soccerField,
      computeEventDate(now, 2, 4), 1, organizers, registeredUsers,
      waitListedUsers, 100, "502904489789649", columbiaParkSoccerWaiverKey);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
    events.add(event);

    organizers = asList(getKey(USER1));
    registeredUsers = asList(getKey(USER2), getKey(USER4), getKey(USER5));
    waitListedUsers = asList();
    event = createEvent("Cook and Serve Dinner", BFHP, bfhpMensShelter,
      computeEventDate(now, 2, 6), 2, organizers, registeredUsers,
      waitListedUsers, 6, "620667924679971", null, bfhpCookAndServeDescription);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
    events.add(event);

    organizers = asList(getKey(USER2));
    registeredUsers = asList(getKey(USER1), getKey(USER4));
    waitListedUsers = asList(getKey(USER3), getKey(USER5));
    event = createEvent("After School Tutoring", BGCSF_TENDERLOIN, bgcsfClubhouse,
      computeEventDate(now, 5, 3), 3, organizers, registeredUsers,
      waitListedUsers, 2, "502905379789560", bgcsfTutoringWaiverKey);
    event.setSuitableForTypes(Lists.newArrayList(SuitableForType.AGE_55_PLUS));
    events.add(event);

    organizers = asList(getKey(AMIR));
    registeredUsers = asList();
    waitListedUsers = asList();
    event = createEvent("Credit Coaching", UNITED_WAY, unitedWayParkmoorOffice,
      computeEventDate(now, 12, 7), 1, organizers, registeredUsers,
      waitListedUsers, 5, null);
    event.setSuitableForTypes(Lists.newArrayList(SuitableForType.GROUPS));
    events.add(event);

    organizers = asList(getKey(HARISH), getKey(POONUM));
    registeredUsers = asList();
    waitListedUsers = asList();
    event = createEvent("Resume Workshop", BENEVOLENT, benevolentParkmoorOffice,
      computeEventDate(now, 12, 7), 1, organizers, registeredUsers,
      waitListedUsers, 5, null);
    event.setSuitableForTypes(Lists.newArrayList(SuitableForType.GROUPS));
    events.add(event);

    Date extraEventStartDate = computeEventDate(now, 20, 0);
    final int numDaysToAddPerIteration = 15;
    for (int extraEventCnt = 0; extraEventCnt < 8; extraEventCnt += 2) {

      organizers = asList(getKey(USER5));
      registeredUsers = asList(getKey(AMIR));
      waitListedUsers = asList();
      event = createEvent("Credit Coaching", UNITED_WAY, unitedWayParkmoorOffice,
        computeEventDate(extraEventStartDate, 0, 5), 1, organizers, registeredUsers,
        waitListedUsers, 5, null);
      events.add(event);

      if ((extraEventCnt/2) % 2 == 0) {
        organizers = asList(getKey(USER5));
        registeredUsers = asList(getKey(HARISH), getKey(POONUM));
        waitListedUsers = asList();
        event = createEvent("Resume Workshop", BENEVOLENT, benevolentParkmoorOffice,
          computeEventDate(extraEventStartDate, 5, 7), 2, organizers, registeredUsers,
          waitListedUsers, 5, null);
        events.add(event);
      } else {
        organizers = asList(getKey(USER1));
        registeredUsers = asList(getKey(USER2), getKey(USER4), getKey(USER5),
          getKey(USER6), getKey(USER7), getKey(USER8), getKey(USER9), getKey(USER10),
          getKey(USER11), getKey(USER12), getKey(USER13),
          getKey(HARISH), getKey(POONUM));
        waitListedUsers = asList();
        event = createEvent("Youth Soccer Clinic", BGCSF_COLUMBIA_PARK, soccerField,
          computeEventDate(extraEventStartDate, 5, 4), 1, organizers, registeredUsers,
          waitListedUsers, 100, null, columbiaParkSoccerWaiverKey);
        event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
        events.add(event);
      }

      extraEventStartDate = computeEventDate(extraEventStartDate, numDaysToAddPerIteration, 0);
    }

    // Past events.
    List<PendingReview> pendingReviews = Lists.newArrayList();
    List<EventNoShowInfo> eventNoShowInfo = Lists.newArrayList();

    organizers = asList(getKey(USER1), getKey(HARISH), getKey(POONUM));
    registeredUsers = asList(getKey(USER2), getKey(USER4), getKey(USER5),
      getKey(USER6), getKey(USER7), getKey(USER8), getKey(USER9), getKey(USER10),
      getKey(USER11), getKey(USER12), getKey(AMIR));
    waitListedUsers = asList(getKey(USER3));
    event = createEvent("Youth Soccer Clinic", BGCSF_TENDERLOIN, soccerField,
      computeEventDate(now, -2, 4), 1,
      organizers, registeredUsers, waitListedUsers, registeredUsers.size(), "502904833122948",
      bgcsfSoccerWaiverKey);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
    event.setImpactSummary(
      "Thanks to all the volunteers that came by to help teach the kids soccer." +
      " The kids really enjoyed learning from everyone and they're eagerly anticipating the" +
      " next event!");
    events.add(event);
    pendingReviews.add(PendingReview.create(event, getKey(USER4),
      "I had a great time teaching soccer to the kids.\n\n" +
      "Thanks to Harish and Poonum for organizing such a wonderful event!", 5));
    pendingReviews.add(PendingReview.create(event, getKey(USER8),
      "Parking was a bit difficult to find...", 3));
    pendingReviews.add(PendingReview.create(event, getKey(USER12), null, 3));
    eventNoShowInfo.add(new EventNoShowInfo(event, asList(getKey(USER2))));

    organizers = asList(getKey(USER1), getKey(AMIR));
    registeredUsers = asList(getKey(USER2), getKey(USER4), getKey(USER5),
      getKey(USER6), getKey(USER7), getKey(USER8), getKey(HARISH), getKey(POONUM));
    waitListedUsers = asList(getKey(USER3));
    event = createEvent("Youth Soccer Clinic", BGCSF_COLUMBIA_PARK, soccerField,
      computeEventDate(now, -5, 4), 1,
      organizers, registeredUsers, waitListedUsers, registeredUsers.size(), "502904726456292",
      columbiaParkSoccerWaiverKey);
    event.setSuitableForTypes(Lists.newArrayList(EnumSet.allOf(SuitableForType.class)));
    event.setImpactSummary(
      "The soccer clinic was a huge success! Thank you to everyone who stopped by.");
    events.add(event);
    pendingReviews.add(PendingReview.create(event, getKey(USER7), null, 4));
    eventNoShowInfo.add(new EventNoShowInfo(event, asList(getKey(USER2))));

    organizers = asList(getKey(USER1));
    registeredUsers = asList(getKey(USER2), getKey(USER5), getKey(HARISH), getKey(POONUM),
      getKey(AMIR));
    waitListedUsers = asList();
    event = createEvent("Cook and Serve Dinner", BFHP, bfhpMensShelter,
      computeEventDate(now, -20, 1), 2, organizers, registeredUsers, waitListedUsers, 6,
      "620667558013341");
    event.setImpactSummary(
        "We successfully prepared and served 76 meals for the homeless in berkeley! Everyone really enjoyed the chicken teryaki and the potstickers. :)");
    events.add(event);

    organizers = asList(getKey(USER1), getKey(HARISH), getKey(POONUM));
    registeredUsers = asList(getKey(USER2), getKey(USER5));
    waitListedUsers = asList();
    event = createEvent("San Francisco Street Cleanup", BENEVOLENT, ferryBuilding,
      computeEventDate(now, -20, 1), 1, organizers, registeredUsers, waitListedUsers, 100,
      "502906933122738");
    event.setImpactSummary(
        "78 Square folks & neighbors brought in 52 bags, a mattress & a chair for a total of " +
        "353 pounds of trash.");
    events.add(event);

    organizers = asList(getKey(USER1), getKey(AMIR));
    registeredUsers = asList(getKey(USER2), getKey(USER5));
    waitListedUsers = asList();
    event = createEvent("San Jose Street Cleanup", UNITED_WAY, unitedWayParkmoorOffice,
      computeEventDate(now, -27, 1), 1, organizers, registeredUsers, waitListedUsers, 100,
      "502906759789422");
    event.setImpactSummary(
      "107 pounds of trash off the street. 7 syringes. 7 thank yous from local residents.");
    events.add(event);

    organizers = asList(getKey(USER1), getKey(HARISH), getKey(POONUM));
    registeredUsers = asList(getKey(USER2), getKey(USER5), getKey(AMIR));
    waitListedUsers = asList();
    event = createEvent("San Francisco Street Cleanup", BENEVOLENT, ferryBuilding,
      computeEventDate(now, -31, 1), 1, organizers, registeredUsers, waitListedUsers, 100,
      "502906933122738");
    events.add(event);
    eventNoShowInfo.add(new EventNoShowInfo(event,
      asList(getKey(AMIR), getKey(USER2))));

    organizers = asList(getKey(USER1), getKey(AMIR));
    registeredUsers = asList(getKey(USER2), getKey(USER5), getKey(HARISH), getKey(POONUM));
    waitListedUsers = asList();
    event = createEvent("San Jose Street Cleanup", UNITED_WAY, unitedWayParkmoorOffice,
      computeEventDate(now, -61, 0), 1, organizers, registeredUsers, waitListedUsers, 100,
      "502906759789422");
    events.add(event);
    eventNoShowInfo.add(new EventNoShowInfo(event,
      asList(getKey(HARISH), getKey(POONUM), getKey(USER2))));

    return new CreateEventsResult(events, eventNoShowInfo, pendingReviews);
  }

  private static Date computeEventDate(Date now, int days, int timePm) {
    Date result = DateUtils.truncate(now, Calendar.DATE);
    result = DateUtils.addHours(result, timePm + 12);
    return DateUtils.addDays(result, days);
  }

  private static Event createEvent(String title, TestOrganization testOrg, Location location,
      Date startTime, int numHours, List<Key<User>> organizers, List<Key<User>> registeredUsers,
      List<Key<User>> waitListedUsers, int maxRegistrations, @Nullable String albumId) {
    return createEvent(title, testOrg, location, startTime, numHours, organizers, registeredUsers,
      waitListedUsers, maxRegistrations, albumId, null);
  }

  private static Event createEvent(String title, TestOrganization testOrg, Location location,
      Date startTime, int numHours, List<Key<User>> organizers, List<Key<User>> registeredUsers,
      List<Key<User>> waitListedUsers, int maxRegistrations, @Nullable String albumId,
      @Nullable Key<Waiver> waiverKey) {
    return createEvent(title, testOrg, location, startTime, numHours, organizers, registeredUsers,
      waitListedUsers, maxRegistrations, albumId, waiverKey, null);
  }

  private static Event createEvent(String title, TestOrganization testOrg, Location location,
      Date startTime, int numHours, List<Key<User>> organizers, List<Key<User>> registeredUsers,
      List<Key<User>> waitListedUsers, int maxRegistrations, @Nullable String albumId,
      @Nullable Key<Waiver> waiverKey, @Nullable String description) {
    eventNum++;
    Event event = new Event();
    event.setTitle(title);
    event.setOrganization(KeyWrapper.create(testOrg.getKey()));

    if (description == null) {
      String eventDescriptionIntro =
          "This is a fabricated event for our online demo. Feel free to register, wait list, etc." +
          " Example event description: ";
      if (eventNum % 2 == 0) {
        event.setDescription(
          eventDescriptionIntro +
          "The Eastmont Garden of Hope receives and distributes donated clothing, hygiene products, household items, canned goods, and other necessities to Social Services Agency clients in urgent need year-round. This program serves as many as 400 households monthly. Whether providing food for a struggling family, a warm coat for a shivering child, or diapers for a desperate mother’s newborn, the Eastmont Garden of Hope seeks to ensure that those in greatest need are served expeditiously and with utmost compassion. Volunteers play a vital role in the program's operations by managing donations and keeping the service area open and available to customers in need throughout the week.\n\n" +
          "Volunteers are requested to participate for at least one shift (3-4 hours) per week, for at least 3 months. Scheduling is flexible, Monday-Friday, between the hours of 9:00am-12:00pm and 1:00-5:00pm.");
        event.setSpecialInstructions("A mandatory Orientation is scheduled for Saturday, June 29, 2013 from 10:00-12:00 at the San Lorenzo Public Library (community meeting room): 395 Paseo Grande, San Lorenzo. Please contact Axxxx Wxxx, coordinator at XXX-XXX-XXXX or xxx@xxx.org to inquire about availability of this position and confirm attendance at orientation.");
        event.setCauses(asList(CauseType.SENIORS));
      } else {
        event.setDescription(
          eventDescriptionIntro +
          "We are seeking volunteers willing to provide in-home respite care for people with terminally ill companion animals. Ancillary services would involve providing some comfort care for the pets themselves as well as pet loss counseling for those who have lost a pet. Risk management and liability issues are still being explored vis-a-vis a possible partnership with our local Humane Society of the North Bay, but we hope to begin training sessions for volunteers in 2014. In the meantime, volunteers will undergo initial screening through The NHFP. Please be patient if you do not hear from us right away. We are generally inundated with requests for emergency pet hospice care as well as involved with training seminars or our biennial symposium. We would appreciate a telephone call from you if you have not heard back from us within four weeks. Please call (XXX) XXX-XXXX and/or leave a message explaining you are a potential volunteer.");
        event.setCauses(asList(CauseType.ANIMALS));
      }
    } else {
      event.setDescription(description);
    }
    event.setLocation(location);
    event.setStartTime(startTime);
    event.setEndTime(DateUtils.addHours(startTime, numHours));
    event.setParticipants(createParticpantsList(organizers, registeredUsers, waitListedUsers));
    event.setMaxRegistrations(maxRegistrations);
    if (albumId != null) {
      event.setAlbum(new AlbumRef(albumId, ImageProviderType.FACEBOOK));
    }
    if (waiverKey != null) {
      event.setWaiver(KeyWrapper.create(waiverKey));
    }
    return event;
  }

  private static List<EventParticipant> createParticpantsList(List<Key<User>> organizers,
      List<Key<User>> registeredUsers, List<Key<User>> waitListedUsers) {
    List<EventParticipant> participants = Lists.newArrayList();
    for (Key<User> organizer : organizers) {
      participants.add(new EventParticipant(organizer, ParticipantType.ORGANIZER));
    }
    for (Key<User> registeredUser : registeredUsers) {
      participants.add(new EventParticipant(registeredUser, ParticipantType.REGISTERED));
    }
    for (Key<User> waitListedUser : waitListedUsers) {
      participants.add(new EventParticipant(waitListedUser, ParticipantType.WAIT_LISTED));
    }
    return participants;
  }

  @Data
  private static class CreateEventsResult {
    private final Collection<Event> events;
    private final List<EventNoShowInfo> eventNoShowInfo;
    // Reviews are persisted after the events the belong to are persisted because reviews are
    // child entities of the events. Meaning the events must be persisted so their event keys
    // can be generated. The event keys will be used as the parent keys for the reviews.
    private final Collection<PendingReview> pendingReviews;
  }

  @Data
  private static class EventNoShowInfo {
    private final Event event;
    private final List<Key<User>> noShowUsers;

    public void persist() {
      for (Key<User> noShowUserKey : noShowUsers) {
        ofy().transact(new UpsertParticipantTxn(
          Key.create(event), noShowUserKey, ParticipantType.REGISTERED_NO_SHOW));
      }
    }
  }

  @Data
  private static class PendingReview {
    private final Event event;
    private final Key<User> userKey;
    private final Review review;

    public static PendingReview create(Event event, Key<User> userKey, @Nullable String comment,
      double ratingValue) {
      return new PendingReview(event, userKey, createReview(comment, ratingValue));
    }

    private static Review createReview(@Nullable String comment, double ratingValue) {
      Review review = new Review();
      if (comment != null) {
        review.setComment(comment);
      }
      review.setRating(Rating.create(ratingValue));
      return review;
    }

    public void persistReview() {
      AdminUtil.executeSubtaskAsUser(userKey, new AdminSubtask() {
        @Override
        public void execute() {
          Event.mutateEventReviewForCurrentUser(Key.create(event), review);
        }
      });
    }
  }

  private void persistOrganizations() {
    for (final TestOrganization testOrg : TestOrganization.values()) {
      // If the organization exists delete it (workaround to reset karma points) and because
      // organizations are no longer deleted during purge.
      Key<Organization> orgKey =
          Organization.createKey(testOrg.getPageName());
      ofy().delete().key(orgKey).now();

      Organization orgDao = createOrganization(testOrg);
      BaseDao.upsert(orgDao);
    }
    // The org is created without any memberships to start with.
    for (TestOrganization testOrg : TestOrganization.values()) {
      if (testOrg.initialAdmin != null) {
        User.updateMembership(getKey(testOrg.initialAdmin), testOrg.getKey(),
          Organization.Role.ADMIN);
      }
      for (TestOrgMembership membership : testOrg.memberships) {
        if (membership.grantedRole != null) {
          User.updateMembership(getKey(membership.user), testOrg.getKey(), membership.grantedRole);
        }
      }
      for (Waiver waiver : testOrg.waivers) {
        Waiver.insert(testOrg.getKey(), waiver);
      }
    }

    for (final TestOrganization testOrg : TestOrganization.values()) {
      for (final TestOrgMembership membership : testOrg.memberships) {
        AdminUtil.executeSubtaskAsUser(getKey(membership.user),
          new AdminSubtask() {
            @Override
            public void execute() {
              if (membership.requestedRole != null) {
                User.updateMembership(getKey(membership.user), testOrg.getKey(),
                  membership.requestedRole);
              }
            }
          });
      }
    }
  }

  private void persistUserManagedEvents() {
    TestUser[] users = {AMIR, HARISH, POONUM};
    Date now = new Date();

    Location galaLocation = new Location(
      "Northbrae Community Church",
      null,
      new Address(
        "941 The Alameda",
        "Berkeley",
        "CA",
        "USA",
        "94707",
        GeoPtWrapper.create(new GeoPt(37.89059f, -122.27611f))));
    String galaDescription =
        "Please help sponsor this wonderful event by BOSS (http://www.self-sufficiency.org/index.php/events) by purchasing tickets online: https://donatenow.networkforgood.org/1439064\n\n"+
        "Details: Celebrate 20 youth who are making positive strides against serious life challenges - homelessness, domestic violence, mental illness or substance abuse in the family. We will honor their achievements at a gala with food, music, awards, and inspiration - please join us!";
    Date galaDate = new GregorianCalendar(2014, GregorianCalendar.JUNE, 5, 18, 0).getTime();

    for (TestUser user : users) {
      UserManagedEvent galaEvent =
          createUserManagedEvent(getKey(user), "1st Annual Rising Stars Youth Leadership Gala",
            galaDescription, galaLocation, galaDate, 2);
      BaseDao.upsert(galaEvent);
    }

    String bfhpDescription =
        "Be part of homeless solutions in Berkeley. Volunteer with the Berkeley Food and Housing Project: http://bfhp.org/get-involved/volunteer/\n\n" +
        "Find out more about volunteering: call our volunteer coordinator at (510) 809-8506 or email mwood@bfhp.org\n\n" +
        "Offer Your Talents\n\n" +
        "Do you have a special talent or skill you could offer our clients? Don't hesitate to call. We have volunteers offer enrichment classes such as yoga, resume reviews and job coaching, and after-school tutoring.\n\n" +
        "Host a Drive\n\n" +
        "BFHP programs and clients are always in need of in-kind donations necessary to maintain proper nutrition, personal hygiene and well-being. Individuals or groups who are interested in organizing collection drives for these items, please consult our Wish List and contact Terrie Light , Executive Director at (510) 809-8507.\n\n" +
        "Bring Your Company or Organization\n\n" +
        "Volunteering with your company provides an opportunity to build morale and demonstrate your company's commitment to the local community. Bring your colleagues or employees into a different environment than usual so they can develop relationships with each other and with the community. Group opportunities include cooking and serving a meal or performing a special facilities project." +
        "Local congregations and community groups form the backbone of our meal service. Sign up for a regular shift with your congregation to cook and serve dinner once a month at our Men's Housing Project. Contact our volunteer coordinator at (510) 809-8506 for more information about volunteering as a group.\n\n";
    for (TestUser user : users) {
      UserManagedEvent bfhpEvent =
          createUserManagedEvent(getKey(user), "Help the Homeless in Berkeley",
            bfhpDescription, null, computeEventDate(now, -10, 6), 2);
      BaseDao.upsert(bfhpEvent);
    }
  }

  private static UserManagedEvent createUserManagedEvent(Key<User> userKey, String title,
      String description, @Nullable Location location, Date startTime, int numHours) {
    UserManagedEvent event = new UserManagedEvent();
    event.setOwner(userKey.getString());
    event.setTitle(title);
    event.setDescription(description);
    event.setLocation(location);
    event.setStartTime(startTime);
    event.setEndTime(DateUtils.addHours(startTime, numHours));
    return event;
  }

  public Organization createOrganization(TestOrganization testOrg) {
    Organization org = new Organization();
    org.setPage(testOrg.getPageRef());
    org.setDonationUrl(testOrg.getDonationUrl());
    if (testOrg.parentOrg != null) {
      org.setParentOrg(new OrganizationNamedKeyWrapper(testOrg.parentOrg.getKey()));
    }
    org.getAutoMembershipRules().addAll(testOrg.autoMembershipRules);
    org.initFromPage();
    return org;
  }

  private void awardBadges(Collection<Event> events) {
    // TODO(avaliani): when organization badges are awarded Badge -> BadgeSummary will not
    // be a sufficient mapping.
    Map<Key<User>, Map<Badge, BadgeSummary>> badgesToAward = Maps.newHashMap();

    List<Key<Event>> pastEventKeys = Lists.newArrayList();
    Date now = new Date();
    for (Event initialEvent : events) {
      if (initialEvent.getStartTime().before(now)) {
        pastEventKeys.add(Key.create(initialEvent));
      }
    }

    Map<Key<Event>, Event> upToDateEvents = ofy().load().keys(pastEventKeys);
    for (Event upToDateEvent : upToDateEvents.values()) {
      for (EventParticipant participant : upToDateEvent.getParticipants()) {

        // Award Padawan badges if the a user has attended at least one event.
        if ((participant.getType() == ParticipantType.ORGANIZER) ||
            (participant.getType() == ParticipantType.REGISTERED) ) {
          Key<User> userKey =
              Key.<User>create(participant.getUser().getKey());
          Map<Badge, BadgeSummary> userBadges =
              badgesToAward.get(userKey);
          if (userBadges == null) {
            userBadges = Maps.newHashMap();
            badgesToAward.put(userKey, userBadges);
          }
          if (!userBadges.containsKey(Badge.KARMA_PADAWAN)) {
            userBadges.put(Badge.KARMA_PADAWAN,
              new BadgeSummary(1, Badge.KARMA_PADAWAN, null));
          }
        }

      }
    }

    // For now award the monthly zen recipient badge to a fixed set of users - history
    // independent. To do this accurately we would need to know the per month karma goal
    // per user.
    List<Key<User>> monthlyZenRecipients = Arrays.asList(
      getKey(AMIR), getKey(HARISH), getKey(POONUM), getKey(USER1));
    for (Key<User> monthlyZenRecipient : monthlyZenRecipients) {
      Map<Badge, BadgeSummary> userBadges =
          badgesToAward.get(monthlyZenRecipient);
      if (userBadges == null) {
        userBadges = Maps.newHashMap();
        badgesToAward.put(monthlyZenRecipient, userBadges);
      }
      userBadges.put(Badge.MONTHLY_ZEN,
        new BadgeSummary(2, Badge.MONTHLY_ZEN, null));
    }

    for (Map.Entry<Key<User>, Map<Badge, BadgeSummary>> entry : badgesToAward.entrySet()) {
      ofy().transact(new UpdateBadgesTxn(
        entry.getKey(), entry.getValue().values()));
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class UpdateBadgesTxn extends VoidWork {
    private final Key<User> userToUpdateKey;
    private final Collection<BadgeSummary> badges;

    public void vrun() {
      User user = ofy().load().key(userToUpdateKey).now();
      if (user == null) {
        throw ErrorResponseMsg.createException("user not found", ErrorInfo.Type.BAD_REQUEST);
      }
      user.setBadges(Lists.newArrayList(badges));
      BaseDao.partialUpdate(user);
    }
  }
}
