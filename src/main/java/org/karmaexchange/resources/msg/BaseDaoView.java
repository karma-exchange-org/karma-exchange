package org.karmaexchange.resources.msg;

import javax.xml.bind.annotation.XmlTransient;

public interface BaseDaoView<T> {

  @XmlTransient
  T getDao();

  // Required to prevent this field from propagating to the json api.
  @XmlTransient
  boolean isKeyComplete();
}
