package org.karmaexchange.dao;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@XmlRootElement
@Entity
@Data
public class Cause {

  @Id private String id;

  public static Cause create(String id) {
    Cause cause = new Cause();
    cause.id = id;
    return cause;
  }
}
