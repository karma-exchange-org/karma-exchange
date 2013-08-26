package org.karmaexchange.task;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Data;

import org.apache.commons.lang3.time.DateUtils;
import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Leaderboard.LeaderboardType;
import org.karmaexchange.task.LeaderboardMapper.UserKarmaRecord;
import org.karmaexchange.util.AdminUtil;
import org.karmaexchange.util.UserService;
import org.karmaexchange.util.AdminUtil.AdminTaskType;

import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

public class LeaderboardReducer extends Reducer<Key<Organization>, UserKarmaRecord, Void> {

  private static final long serialVersionUID = 1L;
  private static final int MAX_LEADERBOARD_SIZE = 10;

  @Override
  public void reduce(Key<Organization> orgKey, ReducerInput<UserKarmaRecord> userKarmaRecords) {
    AdminUtil.setCurrentUser(AdminTaskType.MAP_REDUCE);
    try {
      reduceAsAdmin(orgKey, userKarmaRecords);
    } finally {
      UserService.clearCurrentUser();
    }
  }

  private void reduceAsAdmin(Key<Organization> orgKey,
      ReducerInput<UserKarmaRecord> userKarmaRecords) {
    Date now = new Date();
    Date thirtyDayCutOff = DateUtils.addDays(now, -30);
    Map<Key<User>, LeaderboardScore> allTimeLeaderboardMap = Maps.newHashMap();
    Map<Key<User>, LeaderboardScore> thirtyDayLeaderboardMap = Maps.newHashMap();

    while (userKarmaRecords.hasNext()) {
      UserKarmaRecord record = userKarmaRecords.next();

      addToLeaderboardMap(allTimeLeaderboardMap, record);

      if (record.getEventEndTime().after(thirtyDayCutOff)) {
        addToLeaderboardMap(thirtyDayLeaderboardMap, record);
      }
    }

    List<LeaderboardScore> sortedAllTimeLeaderboardScores =
        sortAndTrimLeaderboard(allTimeLeaderboardMap);
    List<LeaderboardScore> sortedThirtyDayLeaderboardScores =
        sortAndTrimLeaderboard(thirtyDayLeaderboardMap);

    // Load the user objects asynchronously.
    Map<Key<User>, User> allTimeLeaderboardUsers =
        fetchLeaderboardUsers(sortedAllTimeLeaderboardScores);
    Map<Key<User>, User> thirtyDayLeaderboardUsers =
        fetchLeaderboardUsers(sortedThirtyDayLeaderboardScores);

    persistLeaderboard(orgKey, sortedAllTimeLeaderboardScores, allTimeLeaderboardUsers,
      LeaderboardType.ALL_TIME);
    persistLeaderboard(orgKey, sortedThirtyDayLeaderboardScores, thirtyDayLeaderboardUsers,
      LeaderboardType.THIRTY_DAY);
  }

  private void addToLeaderboardMap(Map<Key<User>, LeaderboardScore> leaderboardMap,
      UserKarmaRecord record) {
    LeaderboardScore score = leaderboardMap.get(record.getUserKey());
    if (score == null) {
      leaderboardMap.put(record.getUserKey(), new LeaderboardScore(record));
    } else {
      score.add(record);
    }
  }

  private List<LeaderboardScore> sortAndTrimLeaderboard(
      Map<Key<User>, LeaderboardScore> leaderboadMap) {
    List<LeaderboardScore> sortedScores = Lists.newArrayList(leaderboadMap.values());
    Collections.sort(sortedScores, LeaderboardScore.KarmaPointsComparator.INSTANCE);
    if (sortedScores.size() > MAX_LEADERBOARD_SIZE) {
      sortedScores.subList(MAX_LEADERBOARD_SIZE, sortedScores.size()).clear();
    }
    return sortedScores;
  }

  private Map<Key<User>, User> fetchLeaderboardUsers(List<LeaderboardScore> scores) {
    List<Key<User>> userKeys = Lists.newArrayList();
    for (LeaderboardScore score : scores) {
      userKeys.add(score.getUserKey());
    }
    return ofy().load().keys(userKeys);
  }

  private void persistLeaderboard(Key<Organization> orgKey, List<LeaderboardScore> sortedScores,
      Map<Key<User>, User> usersMap, LeaderboardType type) {
    BaseDao.upsert(new Leaderboard(orgKey, type, sortedScores, usersMap));
  }

  @Data
  public static class LeaderboardScore {
    private final Key<User> userKey;
    private long leaderboardKarmaPoints;

    public LeaderboardScore(UserKarmaRecord record) {
      userKey = record.getUserKey();
      leaderboardKarmaPoints = record.getEventKarmaPoints();
    }

    public void add(UserKarmaRecord record) {
      leaderboardKarmaPoints += record.getEventKarmaPoints();
    }

    public static class KarmaPointsComparator implements Comparator<LeaderboardScore> {
      public static final KarmaPointsComparator INSTANCE = new KarmaPointsComparator();

      @Override
      public int compare(LeaderboardScore score1, LeaderboardScore score2) {
        // Higher scored items come first.
        return Long.compare(score2.leaderboardKarmaPoints, score1.leaderboardKarmaPoints);
      }
    }
  }
}
