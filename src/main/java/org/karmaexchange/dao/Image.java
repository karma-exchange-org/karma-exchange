package org.karmaexchange.dao;

import java.util.Date;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Embed;

@Embed
@ToString
@EqualsAndHashCode
public class Image {

  private BlobKey blobKey;

  /**
   * ImagesService blob url.
   * <p>
   * To get a 32 pixel sized version (aspect-ratio preserved) simply append
   * "=s32" to the url:
   * {@code "http://lh3.ggpht.com/SomeCharactersGoesHere=s32"}
   * <p>
   * To get a 32 pixel cropped version simply append "=s32-c":
   * {@code "http://lh3.ggpht.com/SomeCharactersGoesHere=s32-c"}
   *
   * @see ImagesService#getServingUrl(ServingUrlOptions)
   */
  @Getter
  @Setter
  private String url;
  @Getter
  @Setter
  private ImageProvider urlProvider;
  @Getter
  @Setter
  private KeyWrapper<User> uploadedBy;
  @Getter
  @Setter
  private Date dateUploaded;
  @Getter
  @Setter
  private GeoPtWrapper gpsLocation;  // Most images are tagged with gps information automatically.

  public static Image create(BlobKey blobKey, Key<User> uploadedBy, Date dateUploaded,
                             GeoPt gpsLocation) {
    Image image = new Image();
    image.blobKey = blobKey;
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey).secureUrl(true);
    // This call might be slow. This may have to be punted to a task queue.
    image.setUrl(ImagesServiceFactory.getImagesService().getServingUrl(options));
    image.setUrlProvider(ImageProvider.BLOBSTORE);
    image.setUploadedBy(KeyWrapper.create(uploadedBy));
    image.setDateUploaded(dateUploaded);
    if (gpsLocation != null) {
      image.setGpsLocation(GeoPtWrapper.create(gpsLocation));
    }
    return image;
  }

  public static Image create(String url, ImageProvider provider) {
    Image image = new Image();
    image.setUrl(url);
    image.setUrlProvider(provider);
    return image;
  }

  public String getBlobKey() {
    if (blobKey == null) {
      return null;
    } else {
      return blobKey.getKeyString();
    }
  }

  public void setBlobKey(String blobKeyStr) {
    blobKey = new BlobKey(blobKeyStr);
  }

  public enum ImageProvider {
    FACEBOOK,
    BLOBSTORE
  }
}
