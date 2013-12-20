package org.karmaexchange.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.karmaexchange.dao.Badge;
import org.karmaexchange.dao.CauseType;

import com.google.common.collect.Lists;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public class GenerateDerivedJson {

  public static void main(String[] args) throws Exception {
    File outputDirectory = new File(args[0]);
    outputDirectory.mkdirs();

    DerivedJsonFile[] derivedJsonFiles = {
        PersistedCauseTypeFile.INSTANCE,
        PersistedBadgeFile.INSTANCE
      };

    for (DerivedJsonFile derivedJsonFile : derivedJsonFiles) {
      derivedJsonFile.writeFile(outputDirectory);
    }
  }

  private static abstract class DerivedJsonFile {
    protected abstract String getFileName();
    protected abstract Collection<?> getElements();

    public final void writeFile(File outputDirectory)
        throws Exception {
      FileOutputStream outputDest =
          new FileOutputStream(new File(outputDirectory, getFileName()));
      new ObjectMapper().writeValue(outputDest, getElements());
      outputDest.close();
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class PersistedCauseTypeFile extends DerivedJsonFile {

    public static final PersistedCauseTypeFile INSTANCE = new PersistedCauseTypeFile();

    private static final String CAUSE_TYPES_FILE = "cause-types.json";

    @Override
    protected String getFileName() {
      return CAUSE_TYPES_FILE;
    }

    @Override
    protected Collection<?> getElements() {
      List<PersistedCauseType> persistedCauseTypes = Lists.newArrayList();
      for (CauseType causeType : CauseType.values()) {
        persistedCauseTypes.add(new PersistedCauseType(causeType));
      }
      return persistedCauseTypes;
    }
  }

  @Data
  private static class PersistedCauseType {

    private final String name;
    private final String description;
    private final String searchToken;

    public PersistedCauseType(CauseType causeType) {
      this.name = causeType.name();
      this.description = causeType.getDescription();
      this.searchToken = causeType.getSearchToken();
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class PersistedBadgeFile extends DerivedJsonFile {

    public static final PersistedBadgeFile INSTANCE = new PersistedBadgeFile();

    private static final String BADGES_FILE = "badges.json";

    @Override
    protected String getFileName() {
      return BADGES_FILE;
    }

    @Override
    protected Collection<?> getElements() {
      List<PersistedBadge> persistedBadges = Lists.newArrayList();
      for (Badge badge : Badge.values()) {
        persistedBadges.add(new PersistedBadge(badge));
      }
      return persistedBadges;
    }
  }

  @Data
  private static class PersistedBadge {

    private final String name;
    private final String label;
    private final Badge.Type type;
    private final boolean multipleAwardsAllowed;
    private final String description;

    public PersistedBadge(Badge badge) {
      this.name = badge.name();
      this.label = badge.getLabel();
      this.type = badge.getType();
      this.multipleAwardsAllowed = badge.isMultipleAwardsAllowed();
      this.description = badge.getDescription();
    }
  }
}
