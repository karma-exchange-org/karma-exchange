package org.karmaexchange.task;

import static org.karmaexchange.util.OfyService.ofy;

import java.io.Serializable;
import java.util.Date;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Event.EventParticipant;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.OrganizationNamedKeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Event.Status;
import org.karmaexchange.task.LeaderboardMapper.UserKarmaRecord;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.AdminUtil.AdminTaskType;
import org.karmaexchange.util.UserService;

import lombok.Data;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;
import com.googlecode.objectify.Key;

public class LeaderboardMapper extends Mapper<Entity, Key<Organization>, UserKarmaRecord> {

  private static final long serialVersionUID = 1L;

  @Override
  public void map(Entity dsEvent) {
    AdminUtil.setCurrentUser(AdminTaskType.MAP_REDUCE);
    try {
      mapAsAdmin(dsEvent);
    } finally {
      UserService.clearCurrentUser();
    }
  }

  private void mapAsAdmin(Entity dsEvent) {
    Event event = ofy().toPojo(dsEvent);
    event.processLoad();
    if (event.getStatus() != Status.COMPLETED) {
      return;
    }
    for (OrganizationNamedKeyWrapper associatedOrg : event.getAssociatedOrganizations()) {
      for (EventParticipant participant : event.getParticipants()) {
        if (participant.getType().countAsAttended()) {
          getContext().emit(KeyWrapper.toKey(associatedOrg),
            new UserKarmaRecord(KeyWrapper.toKey(participant.getUser()), event.getKarmaPoints(),
              event.getEndTime()));
        }
      }
    }
  }

  @Data
  public static class UserKarmaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Key<User> userKey;
    private final int eventKarmaPoints;
    private final Date eventEndTime;
  }
}
