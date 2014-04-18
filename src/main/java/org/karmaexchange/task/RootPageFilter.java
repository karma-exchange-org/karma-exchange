package org.karmaexchange.task;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;

public class RootPageFilter implements Filter {

  private static final String ROOT_PAGE_FILE = "app.html";
  private static final Level LOG_LEVEL = Level.FINE;
  private static final Logger log = Logger.getLogger(RootPageFilter.class.getName());

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
      throws IOException {
    HttpServletResponse resp = (HttpServletResponse) servletResp;

    log.log(LOG_LEVEL, "RootPageFilter invoked. Returning " + ROOT_PAGE_FILE);

    resp.setContentType("text/html");
    FileInputStream pageContentStream = new FileInputStream(ROOT_PAGE_FILE);
    long numBytes = ByteStreams.copy(pageContentStream, resp.getOutputStream());
    resp.setContentLength((int) numBytes);
    // Ignore the rest of the filter chain.
  }
}
