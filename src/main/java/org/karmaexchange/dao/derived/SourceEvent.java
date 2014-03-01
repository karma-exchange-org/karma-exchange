package org.karmaexchange.dao.derived;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class SourceEvent extends Event {

  private static long EVENT_ID = 1; // There is a 1-1 mapping between SourceEvent and Event.

  private String sourceKey;

  private List<SourceEventParticipant> sourceParticipants = Lists.newArrayList();

  @Data
  @NoArgsConstructor
  public static final class SourceEventParticipant {
    private SourceKeyWrapper user;
    private ParticipantType type;
  }

  public Event toEvent(Key<Organization> orgKey) {
    validate(orgKey);
    owner = SourceDao.createKey(orgKey, sourceKey);
    id = EVENT_ID;
    // TODO(avaliani): map source participants
    return this;
  }

  public static Key<Event> createKey(Key<Organization> orgKey, String sourceKey) {
    return Key.<Event>create(
      SourceDao.createKey(orgKey, sourceKey),
      Event.class,
      EVENT_ID);
  }

  private void validate(Key<Organization> orgKey) {
    if (sourceKey == null) {
      throw ErrorResponseMsg.createException("sourceKey must be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (getKey() != null) {
      throw ErrorResponseMsg.createException(
        "key is a derived field and can not be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (!KeyWrapper.toKey(getOrganization()).equals(orgKey)) {
      throw ErrorResponseMsg.createException(
        "organization field does not match specified organization",
        ErrorInfo.Type.BAD_REQUEST);
    }
  }
}
