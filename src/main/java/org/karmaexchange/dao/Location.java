package org.karmaexchange.dao;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@XmlRootElement
@Entity
@Data
public class Location {

  @Id private Long id;
  private String title;
  private String description;
  private Address address;

}
