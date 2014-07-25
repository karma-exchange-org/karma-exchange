package org.karmaexchange.dao.derived;

import static org.karmaexchange.util.OfyService.ofy;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.Permission;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
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

  public void init(Key<Organization> orgKey) {
    owner = orgKey;
    id = EVENT_SOURCE_ID;
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

  public static EventSourceInfo validateOrgSecret(String orgId,
      String orgSecret) {
    if (orgId == null) {
      throw ErrorResponseMsg.createException(
        "'orgId' must be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    Key<Organization> orgKey =
        Organization.createKey(orgId);
    EventSourceInfo sourceInfo =
        ofy().load().key(EventSourceInfo.createKey(orgKey)).now();
    if (sourceInfo == null) {
      throw ErrorResponseMsg.createException(
        "organization '" + orgId + "' is not configured to support derived events",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (!sourceInfo.getSecret().equals(orgSecret)) {
      throw ErrorResponseMsg.createException(
        "organization '" + orgId + "' authentication credentials are not valid",
        ErrorInfo.Type.AUTHENTICATION);
    }
    return sourceInfo;
  }
}
