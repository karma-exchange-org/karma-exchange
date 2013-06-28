package org.karmaexchange.task;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class DeleteBlobServlet extends TaskQueueAdminTaskServlet {

  private static final Logger logger = Logger.getLogger(DeleteBlobServlet.class.getName());

  private static final String PATH = "/task/delete_blob";
  private static final String BLOB_KEY_PARAM = "blob_key";

  @Override
  public void execute() throws IOException {
    String blobKeyStr = req.getParameter(BLOB_KEY_PARAM);
    if (blobKeyStr == null) {
      logger.warning("no blob key specified");
    } else {
      BlobKey blobKey;
      try {
        blobKey = new BlobKey(blobKeyStr);
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING, "unable to parse blob key: " + blobKeyStr, e);
        return;
      }
      BlobstoreServiceFactory.getBlobstoreService().delete(blobKey);
    }
  }

  public static void enqueueTask(BlobKey blobKey) {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder.withUrl(DeleteBlobServlet.PATH)
      .param(DeleteBlobServlet.BLOB_KEY_PARAM, blobKey.getKeyString()));
  }
}
