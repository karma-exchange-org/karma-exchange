package org.karmaexchange.dao;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.util.TagUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class CauseType extends NameBaseDao<CauseType> {

  private PageRef page;

  public static CauseType create(String name) {
    return new CauseType(name, null);
  }

  public static CauseType create(String name, PageRef pageRef) {
    return new CauseType(name, pageRef);
  }

  public static Key<CauseType> getKey(String name) {
    return Key.create(CauseType.class, name);
  }

  public static String getTag(Key<CauseType> causeKey) {
    return TagUtil.createTag(getCauseTypeAsString(causeKey));
  }

  public static String getCauseTypeAsString(Key<CauseType> causeKey) {
    return causeKey.getName();
  }

  private CauseType(String name, @Nullable PageRef pageRef) {
    this.name = name;
    page = pageRef;
  }

  @Override
  protected Permission evalPermission() {
    return Permission.READ;
  }
}
