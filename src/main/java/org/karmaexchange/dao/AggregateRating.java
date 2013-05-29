package org.karmaexchange.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class AggregateRating extends Rating {
  private double sum;
  private int count;

  public static AggregateRating create() {
    return new AggregateRating();
  }

  protected AggregateRating() {
    super(null);
  }

  public void addRating(Rating rating) {
    sum += rating.getValue();
    count += 1;
    updateValue();
  }

  public void deleteRating(Rating rating) {
    if (count > 0) {
      sum -= rating.getValue();
      if (sum < 0) {
        sum = 0;
      }
      count -= 1;
      updateValue();
    }
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
