package org.karmaexchange.dao;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.TestUtil.DEBUG;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.util.DatastoreTestUtil;

import com.google.appengine.api.datastore.GeoPt;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;

public class UserTest extends PersistenceTestHelper {

  private User user1;
  private User user2;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    // Setup user1.
    user1 = new User();
    user1.setName("fake name 1");

    ModificationInfo modificationInfo = new ModificationInfo();
    user1.setModificationInfo(modificationInfo);
    modificationInfo.setCreationUser(KeyWrapper.create(user1));
    Date creationDate = new Date();
    modificationInfo.setCreationDate(creationDate);
    modificationInfo.setLastModificationUser(KeyWrapper.create(user1));
    modificationInfo.setLastModificationDate(creationDate);

    Image image = new Image();
    image.setId((long) 1);
    image.setBlobKey("QsZLm_NXs4mCNc2idbG6gg");
    image.setUrl("http://completely_fake");
    image.setUrlProvider(ImageProviderType.BLOBSTORE);
    user1.setProfileImage(ImageRef.create(image));

    user1.getRegisteredEmails().add(new RegisteredEmail("test@karmaexchange.org", true));
    user1.getRegisteredEmails().add(new RegisteredEmail("test2@karmaexchange.org", false));
    Address address = new Address();
    user1.setAddress(address);
    address.setZip("94105");
    address.setGeoPt(GeoPtWrapper.create(new GeoPt(1.9f, 1.0f)));

    EmergencyContact ec1 = new EmergencyContact();
    ec1.setName("Joe");
    ec1.setPhoneNumber("408-123-4567");
    ec1.setRelationship("bff");
    EmergencyContact ec2 = new EmergencyContact();
    ec2.setName("Mary");
    ec2.setPhoneNumber("408-123-4568");
    user1.setEmergencyContacts(asList(ec1, ec2));

    user1.setKarmaPoints(100);

    IndexedAggregateRating  rating = new IndexedAggregateRating ();
    rating.addRating(Rating.create(4.5));
    user1.setEventOrganizerRating(rating);

    List<OAuthCredential> credentials = asList(
      OAuthCredential.create("FACEBOOK", "uid", "token"));
    user1.setOauthCredentials(credentials);

    // Setup user2.
    user2 = new User();
    user2.setName("fake name 2");
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion(user1, User.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(user1);
    validatePersistence(user2);

    // Make sure the unindexed fields are not queryable.
    Set<Key<User>> userKeys = Sets.newHashSet(
      ofy().load().type(User.class)
          .filter("contactInfo.address.zip", Integer.valueOf(94105)).keys());
    assertEquals(0, userKeys.size());

    userKeys = Sets.newHashSet(
      ofy().load().type(User.class)
          .filter("eventOrganizerRating.value >=", Double.valueOf(3.2)).keys());
    assertEquals(1, userKeys.size());
    assertTrue(userKeys.contains(Key.create(user1)));

    userKeys = Sets.newHashSet(
      ofy().load().type(User.class)
          .filter("eventOrganizerRating.value >", Double.valueOf(4.5)).keys());
    assertEquals(0, userKeys.size());

    userKeys = Sets.newHashSet(
      ofy().load()
           .type(User.class)
           .filter("oauthCredentials.globalUidAndToken", "tokenuidFACEBOOK")
           .keys());
    assertEquals(1, userKeys.size());
    assertTrue(userKeys.contains(Key.create(user1)));

    if (DEBUG) {
      DatastoreTestUtil.dumpEntity(user1);
    }
  }
}
