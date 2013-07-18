package org.karmaexchange.resources.msg;

import org.karmaexchange.dao.User;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSummaryInfoView {
  private String firstName;
  private String lastName;
  private String nickName;
  private String key;
  private ImageUrlView profileImage;
  private long karmaPoints;

  public UserSummaryInfoView(User user) {
    firstName = user.getFirstName();
    lastName = user.getLastName();
    nickName = user.getNickName();
    key = user.getKey();
    if (user.getProfileImage() != null) {
      profileImage = ImageUrlView.create(user.getProfileImage());
    }
    karmaPoints = user.getKarmaPoints();
  }
}
