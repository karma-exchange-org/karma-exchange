package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class AggregateRating extends Rating {
  private double sum;
  private int count;

  public static AggregateRating create() {
    return new AggregateRating();
  }

  public static AggregateRating create(@Nullable AggregateRating ratingToCopy) {
    AggregateRating rating = new AggregateRating();
    if (ratingToCopy != null) {
      rating.addAggregateRating(ratingToCopy);
    }
    return rating;
  }

  protected AggregateRating() {
    super(null);
  }

  public void addRating(Rating ratingToAdd) {
    count += 1;
    sum += ratingToAdd.getValue();
    updateValue();
  }

  public void addAggregateRating(AggregateRating ratingToAdd) {
    count += ratingToAdd.count;
    sum += ratingToAdd.sum;
    updateValue();
  }

  public void deleteRating(Rating ratingToDelete) {
    if (count > 0) {
      count -= 1;
      sum -= ratingToDelete.getValue();
      if ((sum < 0) || (count == 0)) {
        sum = 0;
      }
      updateValue();
    }
  }

  public void deleteAggregateRating(AggregateRating ratingToDelete) {
    count -= ratingToDelete.count;
    sum -= ratingToDelete.sum;
    if (count < 0) {
      count = 0;
    }
    if ((sum < 0) || (count == 0)) {
      sum = 0;
    }
    updateValue();
  }

  private void updateValue() {
    if (count == 0) {
      setValue(null);
    } else {
      double value = sum / count;
      if (value > Rating.MAX_RATING) {
        value = Rating.MAX_RATING;
      } else if (value < Rating.MIN_RATING) {
        value = Rating.MIN_RATING;
      }
      setValue(value);
    }
  }
}
