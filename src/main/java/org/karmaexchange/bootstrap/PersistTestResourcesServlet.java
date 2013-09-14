package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.karmaexchange.util.ServletUtil;

@SuppressWarnings("serial")
public class PersistTestResourcesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    PrintWriter statusWriter = resp.getWriter();
    new TestResourcesBootstrapTask(statusWriter, req.getCookies(), getServletContext(),
      ServletUtil.getBaseUri(req)).execute();
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }

}
