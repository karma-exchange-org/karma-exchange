package org.karmaexchange.dao;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RatingTest {

  @Test
  public void testAddRating() {
    Rating rating = new Rating();
    rating.addRating(3);
    rating.addRating(5);
    assertTrue(4.0 == rating.getAverage());
    rating.addRating(3.5);
    rating.addRating(4.5);
    assertTrue(4.0 == rating.getAverage());
  }
}
