package org.karmaexchange.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
@NoArgsConstructor
@AllArgsConstructor
public final class Address {
  private String street;
  private String city;
  private String state;
  private String country;
  private String zip;
  private GeoPtWrapper geoPt;
}
