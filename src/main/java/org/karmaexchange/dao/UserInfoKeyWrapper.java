package org.karmaexchange.dao;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class UserInfoKeyWrapper extends KeyWrapper<User> {
  private String firstName;
  private String lastName;
  private String nickName;
  private ImageUrlView profileImage;
  private long karmaPoints;

  public UserInfoKeyWrapper(User user) {
    super(Key.create(user));
    firstName = user.getFirstName();
    lastName = user.getLastName();
    nickName = user.getNickName();
    if (user.getProfileImage() != null) {
      profileImage = ImageUrlView.create(user.getProfileImage());
    }
    karmaPoints = user.getKarmaPoints();
  }

}
