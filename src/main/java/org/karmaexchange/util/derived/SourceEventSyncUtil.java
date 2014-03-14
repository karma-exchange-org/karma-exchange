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
import org.karmaexchange.dao.SourceEventGeneratorInfo;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.ParticipantType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

public class SourceEventSyncUtil {

  public static void upsertParticipant(Event event, User user, ParticipantType participantType) {
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
        "REGISTER", // TODO(avaliani): fix me
        event.getSourceEventInfo().getSourceKey(),
        user.getFirstName(),
        user.getLastName(),
        "amirvaliani@yahoo.com"); // TODO(avaliani): fix me
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
    private String action;
    private String sourceKey;
    private String firstName;
    private String lastName;
    private String email;
  }

}
