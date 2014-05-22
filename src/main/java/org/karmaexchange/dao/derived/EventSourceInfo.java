package org.karmaexchange.dao.derived;

import static org.karmaexchange.util.OfyService.ofy;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.util.SalesforceUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Cache
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class EventSourceInfo extends IdBaseDao<EventSourceInfo> {

  private static final long EVENT_SOURCE_ID = 1;

  private String secret;
  private String domain;

  public EventSourceInfo(Key<Organization> orgKey, String secret, String domain) {
    owner = orgKey;
    id = EVENT_SOURCE_ID;
    this.secret = secret;
    this.domain = domain;
  }

  @XmlTransient
  public Key<Organization> getOrgKey() {
    return Key.create(owner.getString());
  }

  @XmlTransient
  public String getRegistrationUrl() {
    return "https://" + domain + SalesforceUtil.REGISTRATION_API_PATH;
  }

  @Override
  protected Permission evalPermission() {
    // TODO(avaliani): this is expensive. Need to re-evaluate if this makes sense.
    BaseDao<?> ownerDao = (BaseDao<?>) ofy().load().key(owner).now();
    return ownerDao.getPermission();
  }

  public static Key<EventSourceInfo> createKey(Key<Organization> orgKey) {
    return Key.<EventSourceInfo>create(
      orgKey, EventSourceInfo.class, EVENT_SOURCE_ID);
  }
}
