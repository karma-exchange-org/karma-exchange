package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
public class Location {

  private String title;
  private String description;
  private Address address;

}
