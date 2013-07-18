package org.karmaexchange.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.karmaexchange.util.AdminUtil.AdminTaskType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("serial")
public abstract class AdminTaskServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(AdminTaskServlet.class.getName());

  private final AdminTaskType taskType;

  protected HttpServletRequest req;
  protected HttpServletResponse resp;

  protected abstract void execute() throws IOException;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    this.req = req;
    this.resp = resp;
    AdminUtil.setCurrentUser(taskType);
    try {
      execute();
    } catch (WebApplicationException e) {
      logger.log(Level.WARNING,
        "Admin task failure: " + ServletUtil.getExceptionResponseAsJsonString(e));
    } finally {
      UserService.clearCurrentUser();
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
