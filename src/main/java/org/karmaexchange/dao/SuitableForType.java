package org.karmaexchange.dao;

import org.karmaexchange.util.TagUtil;

public enum SuitableForType {
  KIDS,
  TEENS,
  AGE_55_PLUS,
  GROUPS;

  public String getTag() {
    return TagUtil.createTag("suitablefor" + name());
  }
}
