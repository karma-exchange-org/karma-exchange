package org.karmaexchange.dao;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ServingUrlOptions;

public enum ImageProviderType {
  FACEBOOK,

  /**
   * To get a 32 pixel sized version (aspect-ratio preserved) simply append
   * "=s32" to the url:
   * {@code "http://lh3.ggpht.com/SomeCharactersGoesHere=s32"}
   * <p>
   * To get a 32 pixel cropped version simply append "=s32-c":
   * {@code "http://lh3.ggpht.com/SomeCharactersGoesHere=s32-c"}
   *
   * @see ImagesService#getServingUrl(ServingUrlOptions)
   */
  BLOBSTORE;

  public static ImageProviderType toImageProviderType(
      SocialNetworkProviderType socialNetworkProviderType) {
    return ImageProviderType.valueOf(socialNetworkProviderType.name());
  }
}
