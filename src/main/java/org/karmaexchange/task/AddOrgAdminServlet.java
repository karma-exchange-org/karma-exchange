package org.karmaexchange.task;

import static java.lang.String.format;

import java.io.IOException;
import java.util.logging.Logger;

import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.util.OfyUtil;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class AddOrgAdminServlet extends TaskQueueAdminTaskServlet {

  private static final Logger logger = Logger.getLogger(AddOrgAdminServlet.class.getName());

  private static final String PATH = "/task/add_org_admin";
  private static final String ORG_KEY_PARAM = "org";
  private static final String USER_KEY_PARAM = "user";

  @Override
  protected void execute() throws IOException {
    String orgKeyStr = req.getParameter(ORG_KEY_PARAM);
    String userKeyStr = req.getParameter(USER_KEY_PARAM);
    if ((orgKeyStr == null) || (userKeyStr == null)) {
      logger.warning(format("missing params org='%s' user='%s'", orgKeyStr, userKeyStr));
      return;
    }
    Key<Organization> orgKey = OfyUtil.createKey(orgKeyStr);
    Key<User> userKey = OfyUtil.createKey(userKeyStr);
    User.updateMembership(userKey, orgKey, Organization.Role.ADMIN);
  }

  public static void enqueueTask(Key<Organization> orgKey, Key<User> userKey) {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl(PATH)
      .param(ORG_KEY_PARAM, orgKey.getString())
      .param(USER_KEY_PARAM, userKey.getString()));
  }
}
