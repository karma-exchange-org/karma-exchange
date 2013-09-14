package org.karmaexchange.dao;

import org.karmaexchange.util.SearchUtil.ReservedToken;

import lombok.Getter;

public enum CauseType {
  ANIMALS("Animals"),
  ARTS_AND_CULTURE("Arts & Culture"),
  CHILDREN_AND_YOUTH("Children & Youth"),
  DISASTER_RELIEF("Disaster Relief"),
  EDUCATION("Education"),
  ENVIRONMENT("Environment"),
  FAITH_BASED("Faith-Based"),
  HEALTH_AND_MEDICINE("Health & Medicine"),
  HOMELESSNESS("Homelessness"),
  HUMAN_RIGHTS("Human Rights"),
  HUNGER("Hunger"),
  IMMIGRATION_AND_REFUGEES("Immigration & Refugees"),
  JUSTICE("Justice"),
  LEGAL("Legal"),
  LGBT("LGBT"),
  MENTORSHIP("Mentorship"),
  POLITICS("Politics"),
  PUBLIC_RELATIONS("Public Relations"),
  SENIORS("Seniors"),
  SPORTS_AND_ATHLETICS("Sports & Athletics"),
  TECHNOLOGY("Technology"),
  VETERANS("Veterans"),
  VOLUNTEERING_ABROAD("Volunteering Abroad"),
  WOMEN("Women"),
  WORKFORCE("Workforce");

  @Getter
  private final String description;

  private CauseType(String description) {
    this.description = description;
  }

  public String getSearchToken() {
    return ReservedToken.CAUSE_TYPE.create(description);
  }
}
