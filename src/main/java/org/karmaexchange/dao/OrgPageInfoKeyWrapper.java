package org.karmaexchange.dao;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;

@Embed
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class OrgPageInfoKeyWrapper extends KeyWrapper<Organization> {

  private PageRef page;

  public OrgPageInfoKeyWrapper(Organization org) {
    super(Key.create(org));
    page = org.getPage();
  }
}
