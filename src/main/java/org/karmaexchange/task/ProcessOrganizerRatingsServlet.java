package org.karmaexchange.task;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.karmaexchange.dao.Event;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class ProcessOrganizerRatingsServlet extends AdminTaskServlet {

  private static final Logger logger = Logger.getLogger(
    ProcessOrganizerRatingsServlet.class.getName());

  private static final String PATH = "/task/process_organizer_ratings";
  private static final String EVENT_KEY_PARAM = "event_key";

  @Override
  protected void execute() throws IOException {
    String eventKeyStr = req.getParameter(EVENT_KEY_PARAM);
    if (eventKeyStr == null) {
      logger.warning("no event key specified");
    } else {
      Key<Event> eventKey;
      try {
        eventKey = Key.create(eventKeyStr);
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING, "unable to parse event key: " + eventKeyStr, e);
        return;
      }
      Event.processDerivedOrganizerRatings(eventKey);
    }
  }

  public static void enqueueTask(Event event) {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl(PATH)
      .param(EVENT_KEY_PARAM, Key.create(event).getString()));
  }
}
