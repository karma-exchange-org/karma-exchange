package org.karmaexchange.dao;

import java.util.Date;

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

  private Date awardedOn;
}
