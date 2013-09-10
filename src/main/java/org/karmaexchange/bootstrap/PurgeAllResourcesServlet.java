package org.karmaexchange.bootstrap;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Image;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;

import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class PurgeAllResourcesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/plain");
    PrintWriter statusWriter = resp.getWriter();
    purgeAllResources(statusWriter);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }

  /*
   * The current implementation only works for small dbs. Map-reduce is the right thing for
   * large dbs.
   */
  private void purgeAllResources(PrintWriter statusWriter) {
    statusWriter.println("About to delete all resources...");

    Iterable<Key<Event>> eventKeys = ofy().load().type(Event.class).keys().iterable();
    Iterable<Key<User>> userKeys = ofy().load().type(User.class).keys().iterable();
    Iterable<Key<Organization>> orgKeys = ofy().load().type(Organization.class).keys().iterable();
    Iterable<Key<CauseType>> causeTypeKeys = ofy().load().type(CauseType.class).keys().iterable();
    Iterable<Key<Image>> imageKeys = ofy().load().type(Image.class).keys().iterable();
    Iterable<Key<Review>> reviewKeys = ofy().load().type(Review.class).keys().iterable();
    Iterable<Key<Leaderboard>> leaderboardKeys =
        ofy().load().type(Leaderboard.class).keys().iterable();

    ofy().delete().keys(eventKeys);
    ofy().delete().keys(userKeys);
    ofy().delete().keys(orgKeys);
    ofy().delete().keys(causeTypeKeys);
    ofy().delete().keys(imageKeys);
    ofy().delete().keys(reviewKeys);
    ofy().delete().keys(leaderboardKeys);

    statusWriter.println("Deleted all resources.");
  }
}
