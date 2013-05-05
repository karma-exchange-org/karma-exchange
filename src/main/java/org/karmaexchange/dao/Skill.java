package org.karmaexchange.dao;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@XmlRootElement
@Entity
@Data
public final class Skill {

  /*
   * There is no categorization of skills. Should their be a hierarchy? -
   *   Tech - C++, Java, ++, Medicine - CPR, Physical - Swimming, Soccer.
   */

  @Id private String id;

  public static Skill create(String id) {
    Skill skill = new Skill();
    skill.id = id;
    return skill;
  }
}
