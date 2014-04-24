package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.UserService;
import org.karmaexchange.util.AdminUtil.AdminTaskType;

@SuppressWarnings("serial")
public abstract class BootstrapServlet extends HttpServlet {

  protected HttpServletRequest req;
  protected PrintWriter statusWriter;

  public abstract void execute();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    this.req = req;
    resp.setContentType("text/plain");
    this.statusWriter = resp.getWriter();

    AdminUtil.setCurrentUser(AdminTaskType.BOOTSTRAP);
    try {
      execute();
    } finally {
      UserService.clearCurrentUser();
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
