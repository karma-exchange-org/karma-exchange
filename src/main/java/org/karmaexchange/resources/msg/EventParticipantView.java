package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.KeyWrapper;
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

  public static List<EventParticipantView> get(List<KeyWrapper<User>> usersBatch) {
    List<EventParticipantView> registeredUsers = Lists.newArrayListWithCapacity(usersBatch.size());
    if (!usersBatch.isEmpty()) {
      List<Key<User>> registeredUserKeys = KeyWrapper.getKeyObjs(usersBatch);
      Map<Key<User>, User> registerdUsersMap = ofy().load().keys(registeredUserKeys);
      for (User user : registerdUsersMap.values()) {
        registeredUsers.add(EventParticipantView.create(user));
      }
    }
    return registeredUsers;
  }

  private static EventParticipantView create(User user) {
    EventParticipantView profileImageView = new EventParticipantView();
    profileImageView.setFirstName(user.getFirstName());
    profileImageView.setLastName(user.getLastName());
    profileImageView.setNickName(user.getNickName());
    profileImageView.setKey(user.getKey());
    if (user.getProfileImage() != null) {
      profileImageView.setProfileImage(ImageUrlView.create(user.getProfileImage()));
    }
    profileImageView.setKarmaPoints(user.getKarmaPoints());
    return profileImageView;
  }
}
