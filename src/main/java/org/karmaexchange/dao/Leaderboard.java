package org.karmaexchange.dao;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.task.LeaderboardReducer.LeaderboardScore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;

@XmlRootElement
@Entity
@Cache  // Caching is valuable for the leaderboard since it is fetched by key.
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Leaderboard extends NameBaseDao<Leaderboard> {

  public enum LeaderboardType {
    ALL_TIME,
    THIRTY_DAY
  }

  private List<LeaderboardScoreAndUserInfo> scores = Lists.newArrayList();

  public Leaderboard(Key<Organization> orgKey, LeaderboardType type,
      List<LeaderboardScore> scores, Map<Key<User>, User> usersMap) {
    // Using the same parent causes entity group contention. Which is not needed.
    name = createName(orgKey, type);

    for (LeaderboardScore score : scores) {
      User user = usersMap.get(score.getUserKey());
      if (user != null) {
        this.scores.add(new LeaderboardScoreAndUserInfo(user, score.getLeaderboardKarmaPoints()));
      }
    }
    Collections.sort(this.scores,
      LeaderboardScoreAndUserInfo.KarmaPointsAndUserInfoComparator.INSTANCE);
  }

  private static String createName(Key<Organization> orgKey, LeaderboardType type) {
    return Organization.getUniqueOrgId(orgKey) + type.toString();
  }

  public static Key<Leaderboard> createKey(Key<Organization> orgKey, LeaderboardType type) {
    return Key.<Leaderboard>create(Leaderboard.class, createName(orgKey, type));
  }

  @Override
  protected Permission evalPermission() {
    // Admin tasks create the leaderboard. Everyone else only has read access.
    return Permission.READ;
  }

  @Data
  @NoArgsConstructor
  private static class LeaderboardScoreAndUserInfo {
    private UserInfoKeyWrapper user;
    private long leaderboardKarmaPoints;

    public LeaderboardScoreAndUserInfo(User user, long leaderboardKarmaPoints) {
      this.user = new UserInfoKeyWrapper(user);
      this.leaderboardKarmaPoints = leaderboardKarmaPoints;
    }

    public static class KarmaPointsAndUserInfoComparator
        implements Comparator<LeaderboardScoreAndUserInfo> {

      public static final KarmaPointsAndUserInfoComparator INSTANCE =
          new KarmaPointsAndUserInfoComparator();

      @Override
      public int compare(LeaderboardScoreAndUserInfo score1, LeaderboardScoreAndUserInfo score2) {
        // Higher scored items come first.
        int result = Long.compare(score2.leaderboardKarmaPoints, score1.leaderboardKarmaPoints);
        if (result != 0) {
          return result;
        }
        result = nullSafeIgnoreCaseStringComparator(score1.getUser().getFirstName(),
          score2.getUser().getFirstName());
        if (result != 0) {
          return result;
        }
        return nullSafeIgnoreCaseStringComparator(score1.getUser().getLastName(),
          score2.getUser().getLastName());
      }

      private int nullSafeIgnoreCaseStringComparator(String s1, String s2) {
        if ((s1 == null) || (s2 == null)) {
          return (s1 == null) ? -1 : 1;
        }
        return s1.compareToIgnoreCase(s2);
      }
    }
  }
}
