package org.karmaexchange.resources.msg;

import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.BaseDao;
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
  private List<KeyWrapper<CauseType>> causes;
  // TODO(avaliani): need to expand organizations.
  private KeyWrapper<Organization> organization;

  public static ExpandedEventSearchView create(Event event) {
    // Only fetch the review if the current user is registered for the event.
    Review review = null;
    if (event.getRegistrationInfo() == RegistrationInfo.REGISTERED) {
      review = BaseDao.load(Review.getKeyForCurrentUser(event));
    }
    return new ExpandedEventSearchView(event, review);
  }

  private ExpandedEventSearchView(Event event, @Nullable Review currentUserReview) {
    super(event, currentUserReview);
    description = event.getDescription();

    User user = BaseDao.load(KeyWrapper.toKey(event.getOrganizers().get(0)));
    if (user != null) {
      firstOrganizer = EventParticipantView.create(user);
    }
    numOrganizers = event.getOrganizers().size();

    causes = event.getCauses();
    organization = event.getOrganization();
  }
}
