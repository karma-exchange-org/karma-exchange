package org.karmaexchange.dao;

import static org.karmaexchange.dao.Badge.Type.GENERIC_ONLY;
import static org.karmaexchange.dao.Badge.Type.ORG_ONLY;
import static org.karmaexchange.dao.Badge.Type.GENERIC_AND_ORG;

import lombok.Data;
import lombok.Getter;

public enum Badge {
  KARMA_PADAWAN("Karma Padawan", GENERIC_ONLY, false,
    "Awarded after you attend your first volunteer event.",
    "/img/badges/karma_padawan.png"),

  MONTHLY_ZEN("Monthly Zen", GENERIC_ONLY, true,
    "Awarded when you meet your monthly karma goal.",
    "/img/badges/monthly_zen.png"),

  ZEN_MASTER("Zen Master", GENERIC_ONLY, true,
    "Awarded when you meet your monthly karma goal for one consecutive year.",
    "/img/badges/zen_master.png"),

  DIFFERENCE_MAKER("Difference Maker", ORG_ONLY, false,
    "Awarded when you volunteer with a new organization.",
    "/img/badges/difference_maker.png"),

  GAME_CHANGER("Game Changer", GENERIC_AND_ORG, true,
    "Awarded when you volunteer for 100 hours.",
    "/img/badges/game_changer.png"),

  YOU_CAN_COUNT_ON_ME("You Can Count on Me", GENERIC_ONLY, true,
    "Awarded when you attend 10 events in a row without any no-shows.",
    "/img/badges/you_can_count_on_me.png");

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
  @Getter
  private final Icon icon;

  private Badge(String label, Type type, boolean multipleAwardsAllowed, String description,
      String url) {
    this.label = label;
    this.type = type;
    this.multipleAwardsAllowed = multipleAwardsAllowed;
    this.description = description;
    this.icon = new Icon(url);
  }

  @Data
  public static class Icon {
    private final String url;
  }
}
