package org.karmaexchange.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
@NoArgsConstructor
@AllArgsConstructor
public class Location {

  private String title;
  private String description;
  private Address address;

}
