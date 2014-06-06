package org.karmaexchange.task;

import java.io.IOException;

import org.karmaexchange.dao.Event;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.task.LeaderboardMapper.UserKarmaRecord;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.ServletUtil;

import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.NoOutput;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class ComputeLeaderboardServlet extends TaskQueueAdminTaskServlet {

  private static final int DEFAULT_MAP_SHARD_COUNT = 2;
  private static final int DEFAULT_REDUCE_SHARD_COUNT = 2;
  private static final String PIPELINE_STATUS_PATH = "/_ah/pipeline/status.html";
  private static final String PIPELINE_STATUS_ID_PARAM = "root";

  @Override
  protected void execute() throws IOException {
    redirectToMapReduceStatusUrl(
      startComputeLeaderboardMapReduce());
  }

  public static String startComputeLeaderboardMapReduce() {
    return null;
//    String eventKind = OfyUtil.getKind(Event.class);
//    return MapReduceJob.start(
//        MapReduceSpecification.of(
//            "ComputeLeaderboardMapReduce",
//            new DatastoreInput(eventKind, DEFAULT_MAP_SHARD_COUNT),
//            new LeaderboardMapper(),
//            Marshallers.<Key<Organization>>getSerializationMarshaller(),
//            Marshallers.<UserKarmaRecord>getSerializationMarshaller(),
//            new LeaderboardReducer(),
//            NoOutput.<Void, Void>create(DEFAULT_REDUCE_SHARD_COUNT)),
//        getSettings());
  }

  private static MapReduceSettings getSettings() {
    return null;
//    MapReduceSettings settings = new MapReduceSettings()
//        .setWorkerQueueName("mapreduce-workers")
//        .setControllerQueueName("default");
//    return settings;
  }

  private void redirectToMapReduceStatusUrl(String mapReduceJobId) throws IOException {
    String destinationUrl = getMapReduceStatusUrl(ServletUtil.getBaseUri(req), mapReduceJobId);
    resp.sendRedirect(destinationUrl);
  }

  public static String getMapReduceStatusUrl(String baseUrl, String mapReduceJobId) {
    return baseUrl + PIPELINE_STATUS_PATH + "?" +
        PIPELINE_STATUS_ID_PARAM + "=" + mapReduceJobId;
  }
}
