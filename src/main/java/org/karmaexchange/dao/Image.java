package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.task.DeleteBlobServlet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Image extends IdBaseDao<Image> {

  private BlobKey blobKey;
  private String url;
  private ImageProviderType urlProvider;
  @Index
  private Date dateUploaded;

  private String caption;
  private GeoPtWrapper gpsLocation;  // Most images are tagged with gps information automatically.

  public static Image createAndPersist(Key<?> owner, BlobKey blobKey, String caption) {
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey).secureUrl(true);
    // This call might be slow. This may have to be punted to a task queue.
    String url = ImagesServiceFactory.getImagesService().getServingUrl(options);
    Image image = new Image(owner, url, blobKey, caption);
    BaseDao.upsert(image);
    return image;
  }

  public static Image createAndPersist(Key<?> owner, String url,
      SocialNetworkProviderType socialNetworkUrlProvider) {
    ImageProviderType imageProviderType =
        ImageProviderType.toImageProviderType(socialNetworkUrlProvider);
    Image image = new Image(owner, url, imageProviderType);
    BaseDao.upsert(image);
    return image;
  }

  private Image(Key<?> owner, String url, ImageProviderType urlProvider) {
    this.owner = owner;
    this.url = url;
    this.urlProvider = urlProvider;
  }

  private Image(Key<?> owner, String url, BlobKey blobKey, String caption) {
    this.owner = owner;
    this.blobKey = blobKey;
    this.url = url;
    urlProvider = ImageProviderType.BLOBSTORE;
    this.caption = caption;
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    this.dateUploaded = getModificationInfo().getCreationDate();
  }

  @Override
  protected void processUpdate(Image prevObj) {
    super.processUpdate(prevObj);
    // Some fields can not be modified.
    blobKey = prevObj.blobKey;
    url = prevObj.url;
    urlProvider = prevObj.urlProvider;
    dateUploaded = prevObj.dateUploaded;
  }

  @Override
  protected void processDelete() {
    if (urlProvider == ImageProviderType.BLOBSTORE) {
      // We do this via a task queue so that it is transactionally done. Only if the transaction
      // that contains this commits will this task queue task be executed.
      DeleteBlobServlet.enqueueTask(blobKey);
    }
  }

  public String getBlobKey() {
    return (blobKey == null) ? null : blobKey.getKeyString();
  }

  public void setBlobKey(String blobKeyStr) {
    blobKey = (blobKeyStr == null) ? null : new BlobKey(blobKeyStr);
  }

  @Override
  protected Permission evalPermission() {
    // TODO(avaliani): fill this in. Organizers of events should have
    // the ability to delete pictures also if the picture is owned by an
    // event.
    if (KeyWrapper.toKey(getModificationInfo().getCreationUser()).equals(getCurrentUserKey())) {
      return Permission.ALL;
    } else {
      return Permission.READ;
    }
  }
}
