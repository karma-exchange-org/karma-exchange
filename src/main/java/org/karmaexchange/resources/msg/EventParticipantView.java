package org.karmaexchange.resources.msg;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.User;

import com.google.common.collect.Lists;
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
  private Rating eventOrganizerRating;

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
