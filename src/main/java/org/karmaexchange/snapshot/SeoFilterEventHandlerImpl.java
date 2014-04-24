package org.karmaexchange.snapshot;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import com.github.avaliani.snapshot.SeoFilterEventHandler;
import com.github.avaliani.snapshot.SnapshotResult;

public class SeoFilterEventHandlerImpl implements SeoFilterEventHandler {

  public static final Level LOG_LEVEL = Level.FINE;
  private static final Logger log = Logger.getLogger(SeoFilterEventHandlerImpl.class.getName());

  @Override
  public SnapshotResult beforeSnapshot(HttpServletRequest clientRequest) {
    return lookupSnapshot(clientRequest);
  }

  @Override
  public void afterSnapshot(HttpServletRequest clientRequest, SnapshotResult result) {
    saveSnapshot(clientRequest, result);
  }

  @Override
  public void destroy() {

  }

  @Nullable
  private SnapshotResult lookupSnapshot(HttpServletRequest clientRequest) {
    WebPageSnapshot persistedSnapshot =
        ofy().load().key(WebPageSnapshot.getKey(clientRequest)).now();
    if (persistedSnapshot != null) {
      log.log(LOG_LEVEL, "FOUND cached snapshot result for url: " +
          WebPageSnapshot.getCanonicalUrl(clientRequest));
      return persistedSnapshot.toSnapshotResult();
    } else {
      log.log(LOG_LEVEL, "DID NOT FIND cached snapshot result for url: " +
          WebPageSnapshot.getCanonicalUrl(clientRequest));
      return null;
    }
  }

  private void saveSnapshot(HttpServletRequest clientRequest, SnapshotResult result) {
    WebPageSnapshot persistedSnapshot =
        new WebPageSnapshot(clientRequest, result);
    ofy().save().entity(persistedSnapshot);
    log.log(LOG_LEVEL, "PERSISTED snapshot result for url: " +
        WebPageSnapshot.getCanonicalUrl(clientRequest));
  }
}
