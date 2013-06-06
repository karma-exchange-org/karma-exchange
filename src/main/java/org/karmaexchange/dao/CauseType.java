package org.karmaexchange.dao;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class CauseType extends NameBaseDao<CauseType> {

  private ImageRef image;

  public static CauseType create(String name) {
    return new CauseType(name);
  }

  private CauseType(String name) {
    this.name = name;
  }

  @Override
  protected void updatePermission() {
    setPermission(Permission.READ);
  }
}
