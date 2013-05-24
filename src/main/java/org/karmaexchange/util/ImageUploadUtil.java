package org.karmaexchange.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.common.collect.Lists;

public class ImageUploadUtil {
  // Copy facebook which seems to use 2048.
  private final static int DEFAULT_MAX_IMAGE_PX = 2048;

  public static BlobKey persistImage(HttpServletRequest req) {
    return persistImages(req, 1).get(0);
  }

  public static List<BlobKey> persistImages(HttpServletRequest req, int limit) {
    return persistImages(req, limit, DEFAULT_MAX_IMAGE_PX, DEFAULT_MAX_IMAGE_PX, null);
  }

  public static List<BlobKey> persistImages(HttpServletRequest req, int limit, int maxWidthPx,
      int maxHeightPx, @Nullable MultivaluedMap<String, String> formFields) {
    List<BlobKey> blobKeys = Lists.newArrayList();
    ServletFileUpload servletFileUpload = new ServletFileUpload();
    try {
      FileItemIterator fileIter = servletFileUpload.getItemIterator(req);
      while (fileIter.hasNext()) {
        FileItemStream item = fileIter.next();
        InputStream stream = item.openStream();

        if (item.isFormField()) {
          if (formFields != null) {
            formFields.add(item.getFieldName(), Streams.asString(stream));
          }
        } else {
          Image image = processImage(stream, maxWidthPx, maxHeightPx);
          blobKeys.add(writeToFile(image));
        }
      }
    } catch(IOException e) {
      deleteBlobs(blobKeys);
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    } catch(FileUploadException e) {
      deleteBlobs(blobKeys);
      throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BAD_REQUEST);
    }
    if (blobKeys.size() == 0) {
      throw ErrorResponseMsg.createException(
        "request does not contain any images", ErrorInfo.Type.BAD_REQUEST);
    }
    return blobKeys;
  }

  private static void deleteBlobs(List<BlobKey> blobKeys) {
    BlobstoreServiceFactory.getBlobstoreService().delete(blobKeys.toArray(new BlobKey[0]));
  }

  private static Image processImage(InputStream inputStream, int maxWidthPx, int maxHeightPx)
      throws IOException {
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    Image inputImage = ImagesServiceFactory.makeImage(IOUtils.toByteArray(inputStream));
    Transform resize = ImagesServiceFactory.makeResize(maxWidthPx, maxHeightPx);
    return imagesService.applyTransform(resize, inputImage);
  }

  private static BlobKey writeToFile(Image image) throws IOException {
    FileService fileService = FileServiceFactory.getFileService();
    AppEngineFile file = fileService.createNewBlobFile(toMimeType(image.getFormat()));
    FileWriteChannel writeChannel = fileService.openWriteChannel(file, true);
    writeChannel.write(ByteBuffer.wrap(image.getImageData()));
    writeChannel.closeFinally();
    return fileService.getBlobKey(file);
  }

  private static String toMimeType(Image.Format format) {
    switch (format) {
      case ICO:
        return "image/x-icon";
      default:
        return "image/" + format.name().toLowerCase();
    }
  }
}
