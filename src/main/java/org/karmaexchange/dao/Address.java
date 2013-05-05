package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
public final class Address {
  private String street;
  private String city;
  private String state;
  private String country;
  private Integer zip;
  private GeoPtWrapper geoPt;
}
