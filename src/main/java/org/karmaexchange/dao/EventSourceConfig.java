package org.karmaexchange.dao;

import static org.karmaexchange.util.OfyService.ofy;

import javax.xml.bind.annotation.XmlRootElement;

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
public class EventSourceConfig extends IdBaseDao<EventSourceConfig> {

  private static final long EVENT_SOURCE_ID = 1;

  private String secret;
  private String registrationUrl;

  public EventSourceConfig(Key<Organization> orgKey, String secret, String registrationUrl) {
    owner = orgKey;
    id = EVENT_SOURCE_ID;
    this.secret = secret;
    this.registrationUrl = registrationUrl;
  }

  @Override
  protected Permission evalPermission() {
    // TODO(avaliani): this is expensive. Need to re-evaluate if this makes sense.
    BaseDao<?> ownerDao = (BaseDao<?>) ofy().load().key(owner).now();
    return ownerDao.getPermission();
  }

  public static Key<EventSourceConfig> createKey(Key<Organization> orgKey) {
    return Key.<EventSourceConfig>create(orgKey, EventSourceConfig.class, EVENT_SOURCE_ID);
  }
}
