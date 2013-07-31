package org.karmaexchange.task;

import java.io.IOException;
import java.util.logging.Logger;

import org.karmaexchange.dao.Event;
import org.karmaexchange.util.OfyUtil;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class ProcessRatingsServlet extends TaskQueueAdminTaskServlet {

  private static final Logger logger = Logger.getLogger(
    ProcessRatingsServlet.class.getName());

  private static final String PATH = "/task/process_ratings";
  private static final String EVENT_KEY_PARAM = "event_key";

  @Override
  protected void execute() throws IOException {
    String eventKeyStr = req.getParameter(EVENT_KEY_PARAM);
    if (eventKeyStr == null) {
      logger.warning("no event key specified");
      return;
    }
    Key<Event> eventKey = OfyUtil.createKey(eventKeyStr);
    Event.processDerivedRatings(eventKey);
  }

  public static void enqueueTask(Event event) {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl(PATH)
      .param(EVENT_KEY_PARAM, Key.create(event).getString()));
  }
}
