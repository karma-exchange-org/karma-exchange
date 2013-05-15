package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.Data;

import org.karmaexchange.dao.Image.ImageProvider;

import com.google.common.base.Predicate;
import com.googlecode.objectify.annotation.Embed;

@Embed
@Data
public class ParticipantImage implements Comparable<ParticipantImage> {
  private KeyWrapper<User> participant;
  private String imageUrl;
  private ImageProvider imageUrlProvider;

  @Override
  public int compareTo(ParticipantImage other) {
    return this.participant.compareTo(other.participant);
  }

  public static ParticipantImage create(User participant) {
    ParticipantImage participantImage = new ParticipantImage();
    Image profileImage = participant.getProfileImage();
    participantImage.setParticipant(KeyWrapper.create(participant));
    // Note that every non-test user will have a social network provided url. So there's no
    // reason to return null if the profileImage is null.
    if (profileImage != null) {
      participantImage.setImageUrl(profileImage.getUrl());
      participantImage.setImageUrlProvider(profileImage.getUrlProvider());
    }
    return participantImage;
  }

  public static Predicate<ParticipantImage> createEqualityPredicate(
      final KeyWrapper<User> userKey) {
    return new Predicate<ParticipantImage>() {
      @Override
      public boolean apply(@Nullable ParticipantImage input) {
        return input.getParticipant().equals(userKey);
      }
    };
  }
}
