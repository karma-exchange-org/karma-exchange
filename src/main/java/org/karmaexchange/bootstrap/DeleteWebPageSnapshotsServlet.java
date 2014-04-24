package org.karmaexchange.bootstrap;

import static org.karmaexchange.util.OfyService.ofy;

import org.karmaexchange.snapshot.WebPageSnapshot;

import com.googlecode.objectify.Key;

/*
 * Explicit invocation path:
 *   /bootstrap/delete_snapshots
 */
@SuppressWarnings("serial")
public class DeleteWebPageSnapshotsServlet extends BootstrapServlet {

  @Override
  public void execute() {
    statusWriter.println("About to delete all web page snapshots...");

    Iterable<Key<WebPageSnapshot>> persistedSnapshotKeys =
        ofy().load().type(WebPageSnapshot.class).keys().iterable();
    ofy().delete().keys(persistedSnapshotKeys);

    statusWriter.println("Deleted all web page snapshots.");
  }
}
