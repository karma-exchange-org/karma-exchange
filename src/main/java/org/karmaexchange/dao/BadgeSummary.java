package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
@NoArgsConstructor
@AllArgsConstructor
public class BadgeSummary {
  private int count;

  private Badge badge;
  @Nullable
  private OrgPageInfoKeyWrapper org;

  // TODO(avaliani): awarded on has historical information. When we do automatic badge assignment
  // we will need to store the history somewhere. Therefore once we map that out we can
  // decide whether or not to store awarded on within the badge summary.
  //  private List<Date> awardedOn;
}
