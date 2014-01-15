package org.karmaexchange.bootstrap;

import java.io.PrintWriter;

import org.karmaexchange.resources.EventResource;

@SuppressWarnings("serial")
public class WarmupServlet extends BootstrapTaskServlet {

  @Override
  public BootstrapTask createTask() {
    return new WarmupTask(statusWriter);
  }

}

class WarmupTask extends BootstrapTask {

  public WarmupTask(PrintWriter statusWriter) {
    super(statusWriter);
  }

  @Override
  protected void performTask() {
    statusWriter.println("Warming up event search...");
    EventResource.eventSearchWarmup();
    statusWriter.println("Warmup completed.");
  }
}
