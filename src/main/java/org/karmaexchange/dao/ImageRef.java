package org.karmaexchange.dao;

import javax.annotation.Nullable;


import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Index;

@Data
@Embed
@NoArgsConstructor
public class ImageRef {
  @Index
  KeyWrapper<Image> ref;
  private String url;
  private ImageProviderType urlProvider;

  public static ImageRef create(Image image) {
    return new ImageRef(image);
  }

  private ImageRef(Image image) {
    this.ref = KeyWrapper.create(image);
    this.url = image.getUrl();
    this.urlProvider = image.getUrlProvider();
  }

  public static void updateRefs(Key<Image> oldRef, @Nullable Key<Image> newRef) {
    // TODO(avaliani):
    // - do this in a transactional task queue.
    // - only update refs with events with startTime > now. This will reduce the cost.
  }
}
