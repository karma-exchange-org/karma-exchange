package org.karmaexchange.util;


import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Image;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;

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
    ObjectifyService.register(CauseType.class);
    ObjectifyService.register(Image.class);
    ObjectifyService.register(Review.class);
    ObjectifyService.register(Leaderboard.class);
    // Make sure to update PurgeAllResourcesServlet if a new class is added.
  }

  public static Objectify ofy() {
    return ObjectifyService.ofy();
  }

  public static ObjectifyFactory factory() {
    return ObjectifyService.factory();
  }
}
