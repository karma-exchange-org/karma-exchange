package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.RegistrationInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@XmlRootElement
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class ExpandedEventSearchView extends EventSearchView {
  private String description;

  private EventParticipantView firstOrganizer;
  private int numOrganizers;

  // TODO(avaliani): need to expand causes.
  private List<CauseType> causes;

  public static ExpandedEventSearchView create(Event event) {
    // Only fetch the review if the current user is registered for the event.
    Review review = null;
    if (event.getRegistrationInfo() == RegistrationInfo.REGISTERED) {
      review = ofy().load().key(Review.getKeyForCurrentUser(event)).now();
    }
    Organization org = ofy().load().key(KeyWrapper.toKey(event.getOrganization())).now();
    return new ExpandedEventSearchView(event, org, review);
  }

  private ExpandedEventSearchView(Event event, @Nullable Organization fetchedOrg,
      @Nullable Review currentUserReview) {
    super(event, fetchedOrg, currentUserReview, null);
    description = event.getDescription();

    User user = ofy().load().key(KeyWrapper.toKey(event.getOrganizers().get(0))).now();
    if (user != null) {
      firstOrganizer = EventParticipantView.create(user);
    }
    numOrganizers = event.getOrganizers().size();

    causes = event.getCauses();
  }
}
