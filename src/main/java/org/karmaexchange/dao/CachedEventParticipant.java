package org.karmaexchange.dao;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.base.Predicate;
import com.googlecode.objectify.Key;

/**
 * This class caches the image information of an event participant.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class CachedEventParticipant extends KeyWrapper<User> {
  private CachedImage profileImage;

  public CachedEventParticipant(User participant) {
    super(Key.create(participant));
    ImageRef profileImageRef = participant.getProfileImage();
    if (profileImageRef != null) {
      profileImage = new CachedImage(profileImageRef);
    }
  }

  public static Predicate<CachedEventParticipant> userPredicate(final KeyWrapper<User> userKey) {
    return new Predicate<CachedEventParticipant>() {
      @Override
      public boolean apply(@Nullable CachedEventParticipant input) {
        return input.key.equals(KeyWrapper.toKey(userKey));
      }
    };
  }

  @Data
  @NoArgsConstructor
  public static class CachedImage {
    private String url;
    private ImageProviderType urlProvider;

    public CachedImage(ImageRef imageRef) {
      url = imageRef.getUrl();
      urlProvider = imageRef.getUrlProvider();
    }
  }
}
