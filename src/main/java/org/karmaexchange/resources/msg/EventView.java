package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.karmaexchange.dao.AssociatedOrganization;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;

@XmlRootElement
@Data
@NoArgsConstructor
public class EventView implements BaseDaoView<Event> {

  @Delegate(types={Event.class, IdBaseDao.class, BaseDao.class})
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Event event = new Event();

  private OrgEventSummary sponsoringOrgDetails;

  public EventView(Event event) {
    this(event, true);
  }

  public EventView(Event event, boolean initView) {
    this.event = event;
    if (initView) {
      AssociatedOrganization sponsoringOrgInfo =
          event.getSponsoringOrg();
      Organization sponsoringOrg =
          ofy().load().key(KeyWrapper.toKey(sponsoringOrgInfo)).now();
      if (sponsoringOrg != null) {
        sponsoringOrgDetails = new OrgEventSummary(sponsoringOrg);
      }
    }
  }

  @Override
  public Event getDao() {
    return event;
  }
}
