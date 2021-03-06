package org.karmaexchange.dao;

import lombok.Data;


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
