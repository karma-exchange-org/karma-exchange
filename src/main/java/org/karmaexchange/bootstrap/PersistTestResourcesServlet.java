package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class PersistTestResourcesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    PrintWriter statusWriter = resp.getWriter();
    new CauseTypesBootstrapTask(statusWriter, req.getCookies()).execute();
    new TestResourcesBootstrapTask(statusWriter, req.getCookies()).execute();
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }

}
