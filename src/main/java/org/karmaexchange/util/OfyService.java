package org.karmaexchange.util;


import org.karmaexchange.dao.Cause;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.EventComment;
import org.karmaexchange.dao.NonProfitOrganization;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Skill;
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
    ObjectifyService.register(NonProfitOrganization.class);
    ObjectifyService.register(Skill.class);
    ObjectifyService.register(Cause.class);
    ObjectifyService.register(EventComment.class);
  }

  public static Objectify ofy() {
    return ObjectifyService.ofy();
  }

  public static ObjectifyFactory factory() {
    return ObjectifyService.factory();
  }
}
