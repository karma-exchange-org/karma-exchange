package org.karmaexchange.resources.msg;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;

@Data
@NoArgsConstructor
public class OrgEventSummary {
  private String key;
  private String orgName;
  private PageRef page;
  private long karmaPoints;
  private AggregateRating eventRating;

  public OrgEventSummary(Organization org) {
    key = org.getKey();
    orgName = org.getOrgName();
    page = org.getPage();
    karmaPoints = org.getKarmaPoints();
    eventRating = org.getEventRating();
  }
}
