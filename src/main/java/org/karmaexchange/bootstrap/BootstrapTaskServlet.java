package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public abstract class BootstrapTaskServlet extends HttpServlet {

  protected HttpServletRequest req;
  protected PrintWriter statusWriter;

  public abstract BootstrapTask createTask();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    this.req = req;
    resp.setContentType("text/plain");
    this.statusWriter = resp.getWriter();
    createTask().execute();
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
