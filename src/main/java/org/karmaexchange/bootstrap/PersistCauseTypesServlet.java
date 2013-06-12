package org.karmaexchange.bootstrap;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminTaskType;
import org.karmaexchange.util.UserService;

import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class PersistCauseTypesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    persistCauseTypes(createCauseTypes());
    resp.setContentType("text/plain");
    resp.getWriter().println("Cause types persisted.");
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }

  private static List<CauseType> createCauseTypes() {
    // TODO(harish): this should at the very least be in sync with VolunteerMatch.
    List<CauseType> causeTypes = Lists.newArrayList(
      createCause("Animals", "https://www.facebook.com/110672402288486"),
      createCause("Disabled", "https://www.facebook.com/108479809182565"));
    return causeTypes;
  }

  private static CauseType createCause(String name, String fbPageUrl) {
    return CauseType.create(name, PageRef.create(fbPageUrl, SocialNetworkProviderType.FACEBOOK));
  }

  private static void persistCauseTypes(List<CauseType> causeTypes) {
    AdminUtil.setCurrentUser(AdminTaskType.BOOTSTRAP);
    try {
      for (CauseType causeType : causeTypes) {
        BaseDao.upsert(causeType);
      }
    } finally {
      UserService.clearCurrentUser();
    }
  }
}
