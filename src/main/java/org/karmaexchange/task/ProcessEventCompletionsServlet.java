package org.karmaexchange.task;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.karmaexchange.auth.Session;
import org.karmaexchange.dao.Event;

import com.googlecode.objectify.Key;

/*
 * Explicit invocation path:
 *   /task/process_event_completions
 */
@SuppressWarnings("serial")
public class ProcessEventCompletionsServlet extends TaskQueueAdminTaskServlet {

  private static final Logger logger = Logger.getLogger(
    ProcessEventCompletionsServlet.class.getName());

  @Override
  protected void execute() throws IOException {
    Date now = new Date();

    // TODO(avaliani): Rename this file / or move this code to a separate location.
    Iterable<Key<Session>> expiredSessions = ofy().load().type(Session.class)
        .filter("expiresOn <", now)
        .keys();
    ofy().delete().keys(expiredSessions); // Asynchronously delete any expired sessions.

    // Process event completions
    Iterable<Key<Event>> eventsToComplete = ofy().load().type(Event.class)
        .filter("completionProcessed", false)
        .filter("endTime <", now)
        .keys();
    for (Key<Event> eventKey : eventsToComplete) {
      logger.log(Level.INFO, "Processing event completion: eventKey=" + eventKey.getString());
      Event.processEventCompletionTasks(eventKey);
    }
  }
}
