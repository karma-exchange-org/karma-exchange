package org.karmaexchange.task;

import java.io.IOException;
import java.io.PrintWriter;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.task.LeaderboardMapper.UserKarmaRecord;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.ServletUtil;

import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.appidentity.AppIdentityServiceFailureException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.NoOutput;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class ComputeLeaderboardServlet extends TaskQueueAdminTaskServlet {

  private static final String JOB_NAME = "ComputeLeaderboards";
  private static final String WORKER_QUEUE = "mapreduce-workers";
  private static final int DEFAULT_MAP_SHARD_COUNT = 2;
  private static final int DEFAULT_REDUCE_SHARD_COUNT = 2;
  private static final String PIPELINE_STATUS_PATH = "/_ah/pipeline/status.html";
  private static final String PIPELINE_STATUS_ID_PARAM = "root";

  @Override
  protected void execute() throws IOException {
    String id = startMapReduce();

    // Cron jobs display an error on the app engine console if the return status code is not
    // between 200 and 299.
    resp.setContentType("text/plain");
    PrintWriter statusWriter =
        resp.getWriter();
    statusWriter.println(JOB_NAME + " map reduce initiated: " +
        getMapReduceStatusUrl(id));
  }

  public static String startMapReduce() {
    MapReduceSpecification<Entity, Key<Organization>, UserKarmaRecord, Void, Void> mapReduceSpec =
        createMapReduceSpec();
    MapReduceSettings settings =
        getMapReduceSettings();
    return MapReduceJob.start(mapReduceSpec, settings);
  }

  private static MapReduceSpecification<Entity, Key<Organization>, UserKarmaRecord, Void, Void>
      createMapReduceSpec() {
    String eventKind = OfyUtil.getKind(Event.class);
    return new MapReduceSpecification.Builder<>(
        new DatastoreInput(eventKind, DEFAULT_MAP_SHARD_COUNT),
        new LeaderboardMapper(),
        new LeaderboardReducer(),
        new NoOutput<Void, Void>())
      .setKeyMarshaller(Marshallers.<Key<Organization>>getSerializationMarshaller())
      .setValueMarshaller(Marshallers.<UserKarmaRecord>getSerializationMarshaller())
      .setJobName(JOB_NAME)
      .setNumReducers(DEFAULT_REDUCE_SHARD_COUNT)
      .build();
  }

  private static MapReduceSettings getMapReduceSettings() {
    return new MapReduceSettings.Builder()
      .setBucketName(getGcsBucketName())
      .setWorkerQueueName(WORKER_QUEUE)
//      .setModule(module) // if queue is null will use the current queue or "default" if none
      .build();
  }

  private static String getGcsBucketName() {
    try {
      return AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
    } catch (AppIdentityServiceFailureException ex) {
      // ignore
    }
    return null;
  }

  private String getMapReduceStatusUrl(String mapReduceJobId) {
    String baseUrl = ServletUtil.getBaseUri(req);
    return getMapReduceStatusUrl(baseUrl, mapReduceJobId);
  }

  public static String getMapReduceStatusUrl(String baseUrl, String mapReduceJobId) {
    return baseUrl + PIPELINE_STATUS_PATH + "?" +
        PIPELINE_STATUS_ID_PARAM + "=" + mapReduceJobId;
  }
}
