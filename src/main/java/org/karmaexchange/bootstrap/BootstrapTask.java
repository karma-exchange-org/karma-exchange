package org.karmaexchange.bootstrap;

import java.io.PrintWriter;

import javax.servlet.http.Cookie;

import lombok.RequiredArgsConstructor;

import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.UserService;
import org.karmaexchange.util.AdminUtil.AdminTaskType;

@RequiredArgsConstructor
public abstract class BootstrapTask {

  protected final PrintWriter statusWriter;
  protected final Cookie[] cookies;

  public final void execute() {
    AdminUtil.setCurrentUser(AdminTaskType.BOOTSTRAP);
    try {
      performTask();
    } finally {
      UserService.clearCurrentUser();
    }
  }

  protected abstract void performTask();
}
