package org.karmaexchange.dao;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AggregateRatingTest {

  @Test
  public void testAddRating() {
    AggregateRating rating = new AggregateRating();
    rating.addRating(Rating.create(3));
    rating.addRating(Rating.create(5));
    assertTrue(4.0 == rating.getValue());
    rating.addRating(Rating.create(3.5));
    rating.addRating(Rating.create(4.5));
    assertTrue(4.0 == rating.getValue());
  }
}
