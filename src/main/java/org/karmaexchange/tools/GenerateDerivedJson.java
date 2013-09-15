package org.karmaexchange.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.karmaexchange.dao.CauseType;

import com.google.common.collect.Lists;

import lombok.Data;

public class GenerateDerivedJson {

  public static void main(String[] args) throws Exception {
    File outputDirectory = new File(args[0]);
    outputDirectory.mkdirs();
    PersistedCauseType.writeCauseTypesFile(outputDirectory);
  }

  @Data
  private static class PersistedCauseType {
    private static final String CAUSE_TYPES_FILE = "cause-types.json";

    private final String name;
    private final String description;
    private final String searchToken;

    public static void writeCauseTypesFile(File outputDirectory) throws Exception {
      List<PersistedCauseType> causeTypes =
          PersistedCauseType.generatePersistedCauseTypes();
      FileOutputStream outputDest =
          new FileOutputStream(new File(outputDirectory, CAUSE_TYPES_FILE));
      new ObjectMapper().writeValue(outputDest, causeTypes);
      outputDest.close();
    }

    private static List<PersistedCauseType> generatePersistedCauseTypes() {
      List<PersistedCauseType> persistedCauseTypes = Lists.newArrayList();
      for (CauseType causeType : CauseType.values()) {
        persistedCauseTypes.add(new PersistedCauseType(causeType));
      }
      return persistedCauseTypes;
    }

    private PersistedCauseType(CauseType causeType) {
      this.name = causeType.name();
      this.description = causeType.getDescription();
      this.searchToken = causeType.getSearchToken();
    }
  }
}
