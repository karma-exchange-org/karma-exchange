package org.karmaexchange.resources.msg;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

import lombok.Data;

@XmlRootElement
@Data
public class EventParticipantView {
  private String firstName;
  private String lastName;
  private String nickName;
  private String key;
  private ImageUrlView profileImage;
  private long karmaPoints;
  private AggregateRating eventOrganizerRating;

  public static List<EventParticipantView> get(List<KeyWrapper<User>> usersBatch) {
    List<EventParticipantView> registeredUsers = Lists.newArrayListWithCapacity(usersBatch.size());
    if (!usersBatch.isEmpty()) {
      List<Key<User>> registeredUserKeys = KeyWrapper.toKeys(usersBatch);
      for (User user : BaseDao.load(registeredUserKeys)) {
        registeredUsers.add(EventParticipantView.create(user));
      }
    }
    return registeredUsers;
  }

  public static Map<Key<User>, EventParticipantView> getMap(Collection<Key<User>> usersBatch) {
    Map<Key<User>, EventParticipantView> result = Maps.newHashMap();
    if (!usersBatch.isEmpty()) {
      for (User user : BaseDao.load(usersBatch)) {
        result.put(Key.create(user), EventParticipantView.create(user));
      }
    }
    return result;
  }

  public static EventParticipantView create(User user) {
    EventParticipantView participantView = new EventParticipantView();
    participantView.setFirstName(user.getFirstName());
    participantView.setLastName(user.getLastName());
    participantView.setNickName(user.getNickName());
    participantView.setKey(user.getKey());
    if (user.getProfileImage() != null) {
      participantView.setProfileImage(ImageUrlView.create(user.getProfileImage()));
    }
    participantView.setKarmaPoints(user.getKarmaPoints());
    participantView.setEventOrganizerRating(user.getEventOrganizerRating());
    return participantView;
  }
}
