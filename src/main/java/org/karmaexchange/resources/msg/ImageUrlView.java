package org.karmaexchange.resources.msg;

import lombok.Data;

import org.karmaexchange.dao.Image;
import org.karmaexchange.dao.Image.ImageProvider;

@Data
public class ImageUrlView {
  private String url;
  private ImageProvider urlProvider;

  public static ImageUrlView create(Image image) {
    ImageUrlView imageView = new ImageUrlView();
    imageView.setUrl(image.getUrl());
    imageView.setUrlProvider(image.getUrlProvider());
    return imageView;
  }
}
