package org.karmaexchange.dao;

import javax.annotation.Nullable;

import com.googlecode.objectify.annotation.Embed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embed
@NoArgsConstructor
@AllArgsConstructor
public class AgeRange {
  private int min;
  @Nullable
  private Integer max;
}
