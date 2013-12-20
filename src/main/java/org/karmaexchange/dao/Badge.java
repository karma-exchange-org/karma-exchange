package org.karmaexchange.dao;

import static org.karmaexchange.dao.Badge.Type.GENERIC_ONLY;
import static org.karmaexchange.dao.Badge.Type.ORG_ONLY;
import static org.karmaexchange.dao.Badge.Type.GENERIC_AND_ORG;

import lombok.Getter;

public enum Badge {
  KARMA_PADAWAN("Karma Padawan", GENERIC_ONLY, false,
    "Awarded after you attend your first volunteer event."),

  MONTHLY_ZEN("Monthly Zen", GENERIC_ONLY, true,
    "Awarded when you meet your monthly karma goal."),

  ZEN_MASTER("Zen Master", GENERIC_ONLY, true,
    "Awarded when you meet your monthly karma goal for one consecutive year."),

  DIFFERENCE_MAKER("Difference Maker", ORG_ONLY, false,
    "Awarded when you volunteer with a new organization."),

  GAME_CHANGER("Game Changer", GENERIC_AND_ORG, true,
    "Awarded when you volunteer for 100 hours."),

  YOU_CAN_COUNT_ON_ME("You Can Count on Me", GENERIC_ONLY, true,
    "Awarded when you attend 10 events in a row without any no-shows.");

  public enum Type {
    GENERIC_ONLY,
    ORG_ONLY,
    GENERIC_AND_ORG
  }

  @Getter
  private final String label;
  @Getter
  private final Type type;
  @Getter
  private final boolean multipleAwardsAllowed;
  @Getter
  private final String description;
  // TODO(avaliani): icon details

  private Badge(String label, Type type, boolean multipleAwardsAllowed, String description) {
    this.label = label;
    this.type = type;
    this.multipleAwardsAllowed = multipleAwardsAllowed;
    this.description = description;
  }
}
