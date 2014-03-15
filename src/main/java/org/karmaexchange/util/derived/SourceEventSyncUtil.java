package org.karmaexchange.util.derived;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.codehaus.jackson.map.ObjectMapper;
import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.dao.derived.SourceEventGeneratorInfo;
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

    SourceEventGeneratorInfo config = ofy().load().key(
      SourceEventGeneratorInfo.createKey(
        KeyWrapper.toKey(event.getOrganization()))).now();
    if (config == null) {
      throw ErrorResponseMsg.createException(
        "derived event generator info missing", ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
    }

    URL url;
    try {
      url = new URL(config.getRegistrationUrl());
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

      ObjectMapper mapper = new ObjectMapper();
      RegistrationReq registrationReq = new RegistrationReq(
        action,
        event.getSourceEventInfo().getSourceKey(),
        user.getFirstName(),
        user.getLastName(),
        user.getPrimaryEmail());
      mapper.writeValue(connection.getOutputStream(), registrationReq);

      int responseCode = connection.getResponseCode();
      String responseMsg = connection.getResponseMessage();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        // TODO(avaliani): clean this up
        throw ErrorResponseMsg.createException("Error: " + responseMsg,
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
  private static class RegistrationReq {
    private RegistrationAction action;
    private String sourceKey;
    private String firstName;
    private String lastName;
    private String email;
  }

}
