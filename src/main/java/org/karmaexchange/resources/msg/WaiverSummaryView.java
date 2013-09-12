package org.karmaexchange.resources.msg;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Waiver;

import com.google.common.collect.Lists;

@Data
@NoArgsConstructor
public class WaiverSummaryView {
  private String orgName;
  private String key;
  private String description;
  private Date lastModificationDate;

  private WaiverSummaryView(Organization org, Waiver waiver) {
    orgName = org.getOrgName();
    key = waiver.getKey();
    description = waiver.getDescription();
    lastModificationDate = waiver.getModificationInfo().getLastModificationDate();
  }

  public static List<WaiverSummaryView> create(Organization org, Iterable<Waiver> waivers) {
    List<WaiverSummaryView> waiverSummaries = Lists.newArrayList();
    for (Waiver waiver : waivers) {
      waiverSummaries.add(new WaiverSummaryView(org, waiver));
    }
    return waiverSummaries;
  }

  public static class OrgAndDescriptionComparator implements Comparator<WaiverSummaryView> {
    public static final OrgAndDescriptionComparator INSTANCE = new OrgAndDescriptionComparator();

    @Override
    public int compare(WaiverSummaryView waiver1, WaiverSummaryView waiver2) {
      int val = waiver1.description.compareToIgnoreCase(waiver2.description);
      return (val != 0) ? val : waiver1.orgName.compareToIgnoreCase(waiver2.orgName);
    }
  }
}
