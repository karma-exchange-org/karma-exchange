package org.karmaexchange.task;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.util.logging.Logger;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.util.OfyUtil;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class UpdateNamedKeysAdminTaskServlet extends TaskQueueAdminTaskServlet {

  private static final Logger logger =
      Logger.getLogger(UpdateNamedKeysAdminTaskServlet.class.getName());

  private static final String PATH = "/task/update_named_keys";
  private static final String KEY_PARAM = "key";

  @Override
  protected void execute() throws IOException {
    String keyStr = req.getParameter(KEY_PARAM);
    if (keyStr == null) {
      logger.warning("missing 'key' param");
      return;
    }
    Key<?> key = OfyUtil.createKey(keyStr);
    BaseDao<?> baseDaoObj = (BaseDao<?>) ofy().load().key(key).now();
    baseDaoObj.updateDependentNamedKeys();
  }

  public static <T extends BaseDao<T>> void enqueueTask(Key<T> key) {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl(PATH)
      .param(KEY_PARAM, key.getString()));
  }
}
