package org.karmaexchange.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public final class IndexedAggregateRating extends AggregateRating {

  public static IndexedAggregateRating create() {
    return new IndexedAggregateRating();
  }

  protected IndexedAggregateRating() {
    super();
  }

  @Override
  protected boolean valueIsIndexed() {
    return true;
  }
}
