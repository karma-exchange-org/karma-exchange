package org.karmaexchange.resources.msg;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.Waiver;

@XmlRootElement
@Data
@NoArgsConstructor
public class WaiverView implements BaseDaoView<Waiver> {

  @Delegate(types={Waiver.class, IdBaseDao.class, BaseDao.class})
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Waiver waiver = new Waiver();

  public WaiverView(Waiver waiver) {
    this.waiver = waiver;
  }

  @Override
  public Waiver getDao() {
    return waiver;
  }
}
