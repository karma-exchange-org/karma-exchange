package org.karmaexchange.dao;

import java.util.Date;

import com.googlecode.objectify.annotation.Embed;

import lombok.Data;

@Data
@Embed
public final class ModificationInfo {

  private KeyWrapper<User> creationUser;
  private Date creationDate;

  private KeyWrapper<User> lastModificationUser;
  private Date lastModificationDate;
}
