package org.karmaexchange.util;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.karmaexchange.util.AdminUtil.AdminTaskType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("serial")
public abstract class AdminTaskServlet extends HttpServlet {

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
    } finally {
      UserService.clearCurrentUser();
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }
}
