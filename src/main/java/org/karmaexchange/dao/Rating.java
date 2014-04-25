package org.karmaexchange.dao;

import javax.annotation.Nullable;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.PojoIf;

@Data
@NoArgsConstructor
public class Rating {
  public static final double MIN_RATING = 1;
  public static final double MAX_RATING = 5;

  @Index(ValueIsIndexedCheck.class)
  private Double value;

  public static Rating create(double value) {
    return new Rating(value);
  }

  protected Rating(@Nullable Double value) {
    setValue(value);
  }

  public void setValue(@Nullable Double value) {
    if (value != null) {
      if (value > MAX_RATING) {
        throw ErrorResponseMsg.createException("the max rating value is " + MAX_RATING,
          ErrorInfo.Type.BAD_REQUEST);
      } else if (value < MIN_RATING) {
        throw ErrorResponseMsg.createException("the max rating value is " + MIN_RATING,
          ErrorInfo.Type.BAD_REQUEST);
      }
    }
    this.value = value;
  }

  protected boolean valueIsIndexed() {
    return false;
  }

  private static class ValueIsIndexedCheck extends PojoIf<Rating> {
    @Override
    public boolean matchesPojo(Rating rating) {
      return rating.valueIsIndexed();
    }
  }
}
