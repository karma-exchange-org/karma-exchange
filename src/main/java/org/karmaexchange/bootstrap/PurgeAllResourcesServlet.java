package org.karmaexchange.bootstrap;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.Session;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Image;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserManagedEvent;
import org.karmaexchange.dao.Waiver;
import org.karmaexchange.dao.derived.SourceEventGeneratorInfo;
import org.karmaexchange.snapshot.WebPageSnapshot;

import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class PurgeAllResourcesServlet extends BootstrapServlet {

  /*
   * The current implementation only works for small dbs. Map-reduce is the right thing for
   * large dbs.
   */
  @Override
  public void execute() {
    statusWriter.println("About to delete all resources...");

    Iterable<Key<Event>> eventKeys = ofy().load().type(Event.class).keys().iterable();
    Iterable<Key<User>> userKeys = ofy().load().type(User.class).keys().iterable();
    Iterable<Key<Organization>> orgKeys = ofy().load().type(Organization.class).keys().iterable();
    Iterable<Key<Image>> imageKeys = ofy().load().type(Image.class).keys().iterable();
    Iterable<Key<Review>> reviewKeys = ofy().load().type(Review.class).keys().iterable();
    Iterable<Key<Leaderboard>> leaderboardKeys =
        ofy().load().type(Leaderboard.class).keys().iterable();
    Iterable<Key<Waiver>> waiverKeys = ofy().load().type(Waiver.class).keys().iterable();
    Iterable<Key<SourceEventGeneratorInfo>> generatorInfoKeys =
        ofy().load().type(SourceEventGeneratorInfo.class).keys().iterable();
    Iterable<Key<UserManagedEvent>> userManagedEventKeys =
        ofy().load().type(UserManagedEvent.class).keys().iterable();
    Iterable<Key<WebPageSnapshot>> persistedSnapshotKeys =
        ofy().load().type(WebPageSnapshot.class).keys().iterable();
    Iterable<Key<GlobalUidMapping>> globalUidMappings =
        ofy().load().type(GlobalUidMapping.class).keys().iterable();
    Iterable<Key<Session>> sessions =
        ofy().load().type(Session.class).keys().iterable();
    // Do not delete UserUsage. We want to keep that information even when we reset the demo.

    ofy().delete().keys(eventKeys);
    ofy().delete().keys(userKeys);
    ofy().delete().keys(orgKeys);
    ofy().delete().keys(imageKeys);
    ofy().delete().keys(reviewKeys);
    ofy().delete().keys(leaderboardKeys);
    ofy().delete().keys(waiverKeys);
    ofy().delete().keys(generatorInfoKeys);
    ofy().delete().keys(userManagedEventKeys);
    ofy().delete().keys(persistedSnapshotKeys);
    ofy().delete().keys(globalUidMappings);
    ofy().delete().keys(sessions);
    // Do not delete UserUsage. We want to keep that information even when we reset the demo.

    statusWriter.println("Deleted all resources.");
  }
}
