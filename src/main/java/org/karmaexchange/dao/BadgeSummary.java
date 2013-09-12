package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;

// TODO(avaliani): cleanup post demo.
@Data
@Embed
public class BadgeSummary {
  private int count;
  private String orgName;
  private String description;
  private Icon icon;

  @Data
  @Embed
  public static class Icon {
    String url;
  }
}
