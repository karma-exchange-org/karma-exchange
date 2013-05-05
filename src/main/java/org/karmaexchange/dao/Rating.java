package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Index;

@Data
@Embed
public final class Rating {
  double sum;
  int count;
  @Index
  double average;

  public void addRating(double rating) {
    sum += rating;
    count += 1;
    average = sum / count;
  }
}
