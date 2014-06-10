package org.karmaexchange.resources.derived;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.GlobalUidType;
import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.EventParticipant;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.Event.SourceEventInfo;
import org.karmaexchange.dao.Location;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.dao.derived.SourceEventNamespaceDao;
import org.karmaexchange.dao.IdBaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.GeocodingService;
import org.karmaexchange.util.HtmlUtil;
import org.karmaexchange.util.SalesforceUtil;

import com.google.appengine.api.datastore.GeoPt;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@XmlRootElement
@Data
public class SourceEvent {

  private static long EVENT_ID = 1; // There is a 1-1 mapping between SourceEvent and Event.

  private static final Logger log = Logger.getLogger(SourceEvent.class.getName());

  private String sourceEventId;

  private List<SourceEventParticipant> sourceParticipants = Lists.newArrayList();

  private SourceAssociatedOrg sourceAssociatedOrg;

  private Date sourceLastModifiedDate;

  /*
   * We delegate instead of extending event because Objectify seems to require that all subclasses
   * be entities themselves; which we don't want.
   */
  @Delegate(types={Event.class, IdBaseDao.class, BaseDao.class})
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private Event event = new Event();

  public Event toEvent(EventSourceInfo sourceInfo) {
    validate(sourceInfo.getOrgKey());
    if (event.getDescriptionHtml() != null) {
      event.setDescriptionHtml(
        SalesforceUtil.processRichTextField(event.getDescriptionHtml(), sourceInfo));
    }
    if (event.getLocationInformationHtml() != null) {
      event.setLocationInformationHtml(
        SalesforceUtil.processRichTextField(event.getLocationInformationHtml(), sourceInfo));
    }
    if (event.getExternalRegistrationDetailsHtml() != null) {
      event.setExternalRegistrationDetailsHtml(
        SalesforceUtil.processRichTextField(
          event.getExternalRegistrationDetailsHtml(), sourceInfo));
    }
    if (event.getLocation() != null) {
      Location loc = event.getLocation();
      if (loc.getTitle() != null) {
        loc.setTitle(loc.getTitle().trim());
      }
      if (loc.getDescription() != null) {
        // TODO(avlaiani): support html and non html location description
        loc.setDescription(HtmlUtil.toPlainText(loc.getDescription()).trim());
      }
      Address addr = loc.getAddress();
      if ((addr != null) && (addr.getGeoPt() == null)) {
        String geocodeableAddr = addr.toGeocodeableString();
        // Do a synchronous API call to the Google geocoding service.
        GeoPt geoPt = GeocodingService.getGeoPt(geocodeableAddr);
        if (geoPt != null) {
          addr.setGeoPt(GeoPtWrapper.create(geoPt));
        }
      }
    }
    event.setOwner(
      SourceEventNamespaceDao.createKey(sourceInfo.getOrgKey(), sourceEventId).getString());
    event.setId(EVENT_ID);
    event.setOrganization(KeyWrapper.create(sourceInfo.getOrgKey()));
    event.setSourceEventInfo(new SourceEventInfo(sourceEventId, sourceLastModifiedDate));
    mapSourceParticipants();
    // TODO(avaliani): map source participants
    return event;
  }

  private void mapSourceParticipants() {
    List<Key<GlobalUidMapping>> sourceParticipantMappingsKeys =
        Lists.newArrayList();
    for (SourceEventParticipant sourceParticipant : sourceParticipants) {
      sourceParticipantMappingsKeys.add(
        sourceParticipant.getGlobalUidMappingKey());
    }

    Map<Key<GlobalUidMapping>, GlobalUidMapping> sourceParticipantMappings =
        ofy().load().keys(sourceParticipantMappingsKeys);
    List<EventParticipant> participants =
        Lists.newArrayList();
    for (SourceEventParticipant sourceParticipant : sourceParticipants) {
      GlobalUidMapping mapping =
          sourceParticipantMappings.get(sourceParticipant.getGlobalUidMappingKey());
      Key<User> userKey;
      if (mapping == null) {
        userKey = User.upsertNewUser(
          sourceParticipant.createUser());
      } else {
        userKey = mapping.getUserKey();
      }
      participants.add(sourceParticipant.toEventParticipant(userKey));
    }

    event.setParticipants(participants);
  }

  public static Key<Event> createKey(EventSourceInfo sourceInfo, String sourceKey) {
    return Key.<Event>create(
      SourceEventNamespaceDao.createKey(sourceInfo.getOrgKey(), sourceKey),
      Event.class,
      EVENT_ID);
  }

  private void validate(Key<Organization> orgKey) {
    if (sourceEventId == null) {
      throw ErrorResponseMsg.createException("sourceEventId must be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (event.getKey() != null) {
      throw ErrorResponseMsg.createException(
        "key is a derived field and can not be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (event.getOrganization() != null) {
      throw ErrorResponseMsg.createException(
        "organization field should not be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }

    // This restriction is to simplify the code. In the future we can support a mix of
    // descriptions.
    if (event.getDescription() != null) {
      throw ErrorResponseMsg.createException(
        "only an html description can be specified for derived events",
        ErrorInfo.Type.BAD_REQUEST);
    }

    if (!event.getParticipants().isEmpty()) {
      throw ErrorResponseMsg.createException(
        "only sourceParticipants can be specified for derived events",
        ErrorInfo.Type.BAD_REQUEST);
    }

    Iterator<SourceEventParticipant>  sourceParticipantsIter = sourceParticipants.iterator();
    while (sourceParticipantsIter.hasNext()) {
      SourceEventParticipant sourceParticipant =
          sourceParticipantsIter.next();
      SourceUser sourceUser = sourceParticipant.user;
      if (sourceUser == null) {
        throw ErrorResponseMsg.createException(
          "required field 'user' is null for sourceParticipant",
          ErrorInfo.Type.BAD_REQUEST);
      }
      if (sourceUser.email == null) {
        sourceParticipantsIter.remove();
        log.warning(
          format("required field 'email' missing: " +
              "skipping user(%s,%s) for source event(%s, org=%s)",
            sourceUser.firstName, sourceUser.lastName, sourceEventId,
            orgKey.toString()));
      }
    }
  }

  @Data
  @NoArgsConstructor
  public static final class SourceEventParticipant {
    private SourceUser user;
    private ParticipantType type;

    public Key<GlobalUidMapping> getGlobalUidMappingKey() {
      return GlobalUidMapping.getKey(
        new GlobalUid(GlobalUidType.EMAIL, user.email));
    }

    public EventParticipant toEventParticipant(Key<User> userKey) {
      return EventParticipant.create(userKey, type);
    }

    public UserInfo createUser() {
      User newUser = User.create();
      newUser.setFirstName(user.firstName);
      newUser.setLastName(user.lastName);
      newUser.getRegisteredEmails().add(new RegisteredEmail(user.email, true));
      return new UserInfo(newUser);
    }
  }

  @Data
  @NoArgsConstructor
  public static final class SourceUser {
    private String firstName;
    private String lastName;
    private String email;
  }

  // This is temporary until we decide how to fully incorporate associated orgs.
  @Data
  @NoArgsConstructor
  public static final class SourceAssociatedOrg {
    public enum Type {
      EVENT_SPONSOR
    }

    private String name;
    private Type type;
  }
}
