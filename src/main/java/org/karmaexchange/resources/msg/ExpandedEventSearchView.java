package org.karmaexchange.resources.msg;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Cause;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;

import com.google.common.collect.Lists;

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
  private List<KeyWrapper<Cause>> causes;
  // TODO(avaliani): need to expand organizations.
  private List<KeyWrapper<Organization>> organizations = Lists.newArrayList();

  public static ExpandedEventSearchView create(Event event) {
    return new ExpandedEventSearchView(event);
  }

  private ExpandedEventSearchView(Event event) {
    super(event);
    description = event.getDescription();

    User user = BaseDao.load(KeyWrapper.toKey(event.getOrganizers().get(0)));
    if (user != null) {
      firstOrganizer = EventParticipantView.create(user);
    }
    numOrganizers = event.getOrganizers().size();

    causes = event.getCauses();
    organizations = event.getOrganizations();
  }
}
