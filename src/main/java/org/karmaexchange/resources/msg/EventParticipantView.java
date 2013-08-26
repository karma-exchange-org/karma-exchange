package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@XmlRootElement
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class EventParticipantView extends UserSummaryInfoView {
  private AggregateRating eventOrganizerRating;

  public static List<EventParticipantView> get(List<KeyWrapper<User>> usersBatch) {
    List<EventParticipantView> registeredUsers = Lists.newArrayListWithCapacity(usersBatch.size());
    if (!usersBatch.isEmpty()) {
      List<Key<User>> registeredUserKeys = KeyWrapper.toKeys(usersBatch);
      for (User user : ofy().load().keys(registeredUserKeys).values()) {
        if (user != null) {
          registeredUsers.add(EventParticipantView.create(user));
        }
      }
    }
    return registeredUsers;
  }

  public static Map<Key<User>, EventParticipantView> getMap(Collection<Key<User>> usersBatch) {
    Map<Key<User>, EventParticipantView> result = Maps.newHashMap();
    if (!usersBatch.isEmpty()) {
      for (User user : ofy().load().keys(usersBatch).values()) {
        if (user != null) {
          result.put(Key.create(user), EventParticipantView.create(user));
        }
      }
    }
    return result;
  }

  public static EventParticipantView create(User user) {
    return new EventParticipantView(user);
  }

  public EventParticipantView(User user) {
    super(user);
    eventOrganizerRating = user.getEventOrganizerRating();
  }
}
