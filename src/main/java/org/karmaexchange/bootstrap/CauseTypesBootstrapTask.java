package org.karmaexchange.bootstrap;

import java.io.PrintWriter;
import java.util.Collection;

import javax.servlet.http.Cookie;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import com.google.common.collect.Lists;

/**
 * This class persists a set of pre-defined cause types to the datastore.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
public class CauseTypesBootstrapTask extends BootstrapTask {

  public CauseTypesBootstrapTask(PrintWriter statusWriter, Cookie[] cookies) {
    super(statusWriter, cookies);
  }

  @Override
  protected void performTask() {
    statusWriter.println("About to persist cause types...");
    for (CauseType causeType : createCauseTypes()) {
      BaseDao.upsert(causeType);
    }
    statusWriter.println("Cause types persisted.");
  }

  private static Collection<CauseType> createCauseTypes() {
    // TODO(harish): this should at the very least be in sync with VolunteerMatch.
    return Lists.newArrayList(
      createCause("Animals", "https://www.facebook.com/110672402288486"),
      createCause("Disabled", "https://www.facebook.com/108479809182565"));
  }

  private static CauseType createCause(String name, String fbPageUrl) {
    return CauseType.create(name, PageRef.create(fbPageUrl, SocialNetworkProviderType.FACEBOOK));
  }
}
