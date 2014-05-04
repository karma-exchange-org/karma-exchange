package org.karmaexchange.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BootstrapPropertiesFilter implements Filter {
  /*
   * This filter bootstraps the Properties class so that it doesn't require the request
   * object whenever it is invoked.
   */

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
  public void doFilter(ServletRequest servletReq, ServletResponse servletResp, FilterChain filters)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) servletReq;
    HttpServletResponse resp = (HttpServletResponse) servletResp;

    Properties.requestStart(req);
    try {
      filters.doFilter(req, resp);
    } finally {
      Properties.requestEnd();
    }
  }

}
