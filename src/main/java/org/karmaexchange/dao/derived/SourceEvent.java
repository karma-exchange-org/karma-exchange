package org.karmaexchange.dao.derived;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.Event.SourceEventInfo;
import org.karmaexchange.dao.EventSourceConfig;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@XmlRootElement
@Data
public class SourceEvent {

  private static long EVENT_ID = 1; // There is a 1-1 mapping between SourceEvent and Event.

  private String sourceKey;

  private List<SourceEventParticipant> sourceParticipants = Lists.newArrayList();

  /*
   * We delegate instead of extending event because Objectify seems to require that all subclasses
   * be entities themselves; which we don't want.
   */
  @Delegate(types={Event.class, IdBaseDao.class, BaseDao.class})
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Event event = new Event();

  public Event toEvent(Key<Organization> orgKey, EventSourceConfig sourceConfig) {
    validate(orgKey);
    event.setOwner(SourceDao.createKey(orgKey, sourceKey).getString());
    event.setId(EVENT_ID);
    event.setSourceEventInfo(new SourceEventInfo(sourceConfig, sourceKey));
    // TODO(avaliani): map source participants
    return event;
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
    if (event.getKey() != null) {
      throw ErrorResponseMsg.createException(
        "key is a derived field and can not be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (!KeyWrapper.toKey(event.getOrganization()).equals(orgKey)) {
      throw ErrorResponseMsg.createException(
        "organization field does not match specified organization",
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  @Data
  @NoArgsConstructor
  public static final class SourceEventParticipant {
    private SourceKeyWrapper user;
    private ParticipantType type;
  }
}
