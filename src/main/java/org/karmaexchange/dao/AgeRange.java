package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgeRange {
  private int min;
  @Nullable
  private Integer max;
}
