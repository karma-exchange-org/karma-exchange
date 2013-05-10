package org.karmaexchange.resources.msg;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.User;

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

  public static EventParticipantView create(User user) {
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
