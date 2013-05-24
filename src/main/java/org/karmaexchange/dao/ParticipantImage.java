package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.Data;

import org.karmaexchange.dao.Image.ImageProviderType;

import com.google.common.base.Predicate;
import com.googlecode.objectify.annotation.Embed;

@Embed
@Data
public class ParticipantImage {
  private KeyWrapper<User> participant;
  private String imageUrl;
  private ImageProviderType imageUrlProvider;

  public static ParticipantImage create(User participant) {
    ParticipantImage participantImage = new ParticipantImage();
    ImageRef profileImageRef = participant.getProfileImage();
    participantImage.setParticipant(KeyWrapper.create(participant));
    if (profileImageRef != null) {
      participantImage.setImageUrl(profileImageRef.getUrl());
      participantImage.setImageUrlProvider(profileImageRef.getUrlProvider());
    }
    return participantImage;
  }

  public static Predicate<ParticipantImage> userPredicate(final KeyWrapper<User> userKey) {
    return new Predicate<ParticipantImage>() {
      @Override
      public boolean apply(@Nullable ParticipantImage input) {
        return input.getParticipant().equals(userKey);
      }
    };
  }
}
