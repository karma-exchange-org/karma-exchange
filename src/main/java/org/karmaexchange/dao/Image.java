package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.task.DeleteBlobServlet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

// TODO(avaliani):
// 1. Test moving modificationInfo, permission, and key to BaseDao. Make them protected.
//    Can we somehow move setId() to BaseDao. Or is that even needed. In the update
//    flow isn't the key eventually updated. So is it important that it be immediately updated?
//   See if:
//   a) Objectify handles it properly.
//   b) Json conversion handles it properly.
//   Note that equalsAndHashCode would then need to call super.
//
// 1.b. Re-eval setId() overriding.
@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class Image extends BaseDao<Image> {

  @Parent
  Key<?> owner;
  @Id
  private Long id;
  @Ignore
  private String key;
  private ModificationInfo modificationInfo;

  @Ignore
  private Permission permission;

  private BlobKey blobKey;
  private String url;
  private ImageProviderType urlProvider;
  @Index
  private Date dateUploaded;

  private String caption;
  private GeoPtWrapper gpsLocation;  // Most images are tagged with gps information automatically.

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
    this.dateUploaded = modificationInfo.getCreationDate();
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

  @Override
  public void setId(Long id) {
    this.id = id;
    updateKey();
  }

  public void setOwner(String keyStr) {
    owner = Key.<Object>create(keyStr);
  }

  public String geOwner() {
    return owner.getString();
  }

  public String getBlobKey() {
    return (blobKey == null) ? null : blobKey.getKeyString();
  }

  public void setBlobKey(String blobKeyStr) {
    blobKey = (blobKeyStr == null) ? null : new BlobKey(blobKeyStr);
  }

  @Override
  protected void updatePermission() {
    // TODO(avaliani): fill this in. Organizers of events should have
    // the ability to delete pictures also if the picture is owned by an
    // event.
    if (KeyWrapper.toKey(modificationInfo.getCreationUser()).equals(getCurrentUserKey())) {
      permission = Permission.ALL;
    } else {
      permission = Permission.READ;
    }
  }
}
