package org.karmaexchange.task;

import org.karmaexchange.util.AdminTaskServlet;
import org.karmaexchange.util.AdminUtil;

@SuppressWarnings("serial")
public abstract class TaskQueueAdminTaskServlet extends AdminTaskServlet {

  public TaskQueueAdminTaskServlet() {
    super(AdminUtil.AdminTaskType.TASK_QUEUE);
  }
}
