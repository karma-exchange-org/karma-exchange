package org.karmaexchange.util;


import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.Session;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Image;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserManagedEvent;
import org.karmaexchange.dao.UserUsage;
import org.karmaexchange.dao.Waiver;
import org.karmaexchange.dao.derived.SourceEventGeneratorInfo;
import org.karmaexchange.dao.derived.SourceEventNamespaceDao;
import org.karmaexchange.snapshot.WebPageSnapshot;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

public class OfyService {

  /**
   * Objectify classes persisted to the datastore must be registered here.
   */
  static {
    ObjectifyService.register(Event.class);
    ObjectifyService.register(User.class);
    ObjectifyService.register(Organization.class);
    ObjectifyService.register(Image.class);
    ObjectifyService.register(Review.class);
    ObjectifyService.register(Leaderboard.class);
    ObjectifyService.register(Waiver.class);
    ObjectifyService.register(UserUsage.class);
    ObjectifyService.register(SourceEventNamespaceDao.class); // does not need to be purged. Never peristed.
    ObjectifyService.register(SourceEventGeneratorInfo.class);
    ObjectifyService.register(UserManagedEvent.class);
    ObjectifyService.register(WebPageSnapshot.class);
    ObjectifyService.register(GlobalUidMapping.class);
    ObjectifyService.register(Session.class);
    // Make sure to update PurgeAllResourcesServlet if a new class is added.
  }

  public static Objectify ofy() {
    return ObjectifyService.ofy();
  }

  public static ObjectifyFactory factory() {
    return ObjectifyService.factory();
  }
}
