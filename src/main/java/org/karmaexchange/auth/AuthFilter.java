package org.karmaexchange.auth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.karmaexchange.util.ServletUtil;
import org.karmaexchange.util.UserService;

public class AuthFilter implements Filter {

  public static final Level OAUTH_LOG_LEVEL = Level.FINE;
  private static final Logger log = Logger.getLogger(AuthFilter.class.getName());

  @SuppressWarnings("unused")
  private FilterConfig filterConfig;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  @Override
  public void destroy() {
    filterConfig = null;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filters)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    Session session;
    try {
      session = Session.getCurrentSession(req);
    } catch (WebApplicationException e) {
      Response errMsg = e.getResponse();
      log.log(OAUTH_LOG_LEVEL, "Failed to lookup credentials:\n  " + errMsg.getEntity());
      ServletUtil.setResponse(resp, e);
      return;
    }

    // Authorization complete. Continue to the API entry point.
    if (session != null) {
      UserService.setCurrentUser(session.getUserKey());
    }
    try {
      filters.doFilter(req, resp);
    } finally {
      if (session != null) {
        UserService.clearCurrentUser();
      }
    }
  }

}
