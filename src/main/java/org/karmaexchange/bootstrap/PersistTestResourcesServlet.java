package org.karmaexchange.bootstrap;

@SuppressWarnings("serial")
public class PersistTestResourcesServlet extends BootstrapTaskServlet {

  @Override
  public BootstrapTask createTask() {
    return new TestResourcesBootstrapTask(statusWriter);
  }

}
