package org.karmaexchange.util.derived;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.resources.derived.SourceEvent;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

public class SourceEventSyncUtil {

  private enum RegistrationAction {
    REGISTER,
    UNREGISTER
  }

  public static void upsertParticipant(Key<Event> eventKey, Key<User> userKey,
      ParticipantType participantType) {
    // TODO(avaliani): handle other states
    if (participantType != ParticipantType.REGISTERED) {
      return;
    }
    syncRegistration(eventKey, userKey, RegistrationAction.REGISTER);
  }

  public static void deleteParticipant(Key<Event> eventKey, Key<User> userKey) {
    syncRegistration(eventKey, userKey, RegistrationAction.UNREGISTER);
  }

  private static void syncRegistration(Key<Event> eventKey, Key<User> userKey,
      RegistrationAction action) {
    Event event = ofy().load().key(eventKey).now();
    if (event == null) {
      throw ErrorResponseMsg.createException("event not found",
        ErrorInfo.Type.BAD_REQUEST);
    }
    User user = ofy().load().key(userKey).now();
    if (user == null) {
      throw ErrorResponseMsg.createException("user not found",
        ErrorInfo.Type.BAD_REQUEST);
    }

    if (event.getSourceEventInfo() == null) {
      return;
    }

    EventSourceInfo sourceInfo = ofy().load().key(
      EventSourceInfo.createKey(
        KeyWrapper.toKey(event.getOrganization()))).now();
    if (sourceInfo == null) {
      throw ErrorResponseMsg.createException(
        "derived event generator info missing", ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
    }

    URL url;
    try {
      url = new URL(sourceInfo.getRegistrationUrl());
    } catch (MalformedURLException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Content-type", "application/json");
      connection.setReadTimeout(0);

      ObjectMapper mapper = new ObjectMapper();
      RegistrationRequest registrationReq = new RegistrationRequest(
        sourceInfo.getSecret(),
        action,
        event.getSourceEventInfo().getEventId(),
        user.getFirstName(),
        user.getLastName(),
        user.getPrimaryEmail());
      mapper.writeValue(connection.getOutputStream(), registrationReq);

      int responseCode = connection.getResponseCode();
      StringWriter responseContent = new StringWriter();
      IOUtils.copy(
        connection.getInputStream(), responseContent);
      if (responseCode == HttpURLConnection.HTTP_OK) {
        RegistrationResponse registrationResp =
            mapper.readValue(responseContent.toString(), RegistrationResponse.class);
        // TODO(avaliani): leverage the sourceEvent to do a full event update (vs. explicitly
        // processing the update). The update should happen even on error. We can be out of
        // sync we need to update our db.
        if (registrationResp.error != null) {
          registrationResp.error.convertAndThrowError();
        }
      } else {
        throw ErrorResponseMsg.createException(
          format("Error: remote db request failed [%s, %d, %s,\n response='%s'\n]",
            url,
            connection.getResponseCode(),
            connection.getResponseMessage(),
            responseContent),
          ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
      }
    } catch (IOException e) {
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.PARTNER_SERVICE_FAILURE);
    }
  }

  @XmlRootElement
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class RegistrationRequest {
    private String secretKey;

    private RegistrationAction action;
    private String eventId;
    private String firstName;
    private String lastName;
    private String email;
  }

  @XmlRootElement
  @Data
  private static class RegistrationResponse {
    SourceEvent sourceEvent;
    SourceErrorInfo error;
  }

  @Data
  private static class SourceErrorInfo {
    public enum ErrorType {
      AUTHENTICATION_FAILURE,
      INVALID_PARAM,
      REGISTRATION_LIMIT_REACHED,
      OBJECT_NOT_FOUND,
      DML_EXCEPTION,
      OTHER_EXCEPTION
    }

    private ErrorType type;
    private String message;

    public void convertAndThrowError() {
      if (type == ErrorType.REGISTRATION_LIMIT_REACHED) {
        throw ErrorResponseMsg.createException(
          "the event has reached the max registration limit",
          ErrorInfo.Type.LIMIT_REACHED);
      } else if (type == ErrorType.OBJECT_NOT_FOUND) {
        throw ErrorResponseMsg.createException("event not found",
          ErrorInfo.Type.BAD_REQUEST);
      } else {
        throw ErrorResponseMsg.createException(
          format("Error: exception updating remote db [%s,%s]", type, message),
          ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
      }
    }
  }
}
