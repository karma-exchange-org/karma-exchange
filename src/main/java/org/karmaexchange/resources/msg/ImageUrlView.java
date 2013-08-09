package org.karmaexchange.resources.msg;

import lombok.Data;

import org.karmaexchange.dao.ImageProviderType;
import org.karmaexchange.dao.ImageRef;

@Data
public class ImageUrlView {
  private String url;
  private ImageProviderType urlProvider;

  public static ImageUrlView create(ImageRef image) {
    ImageUrlView imageView = new ImageUrlView();
    imageView.setUrl(image.getUrl());
    imageView.setUrlProvider(image.getUrlProvider());
    return imageView;
  }
}
