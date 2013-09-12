package org.karmaexchange.resources.msg;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.dao.Waiver;

import com.google.common.collect.Lists;

@Data
@NoArgsConstructor
public class WaiverSummaryView {
  private String key;
  private String description;
  private Date lastModificationDate;

  private WaiverSummaryView(Waiver waiver) {
    key = waiver.getKey();
    description = waiver.getDescription();
    lastModificationDate = waiver.getModificationInfo().getLastModificationDate();
  }

  public static List<WaiverSummaryView> create(Iterable<Waiver> waivers) {
    List<WaiverSummaryView> waiverSummaries = Lists.newArrayList();
    for (Waiver waiver : waivers) {
      waiverSummaries.add(new WaiverSummaryView(waiver));
    }
    return waiverSummaries;
  }

  public static class DescriptionComparator implements Comparator<WaiverSummaryView> {
    public static final DescriptionComparator INSTANCE = new DescriptionComparator();

    @Override
    public int compare(WaiverSummaryView waiver1, WaiverSummaryView waiver2) {
      return waiver1.description.compareToIgnoreCase(waiver2.description);
    }
  }
}
