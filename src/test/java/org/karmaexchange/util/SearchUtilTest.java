package org.karmaexchange.util;

import static org.junit.Assert.*;
import static org.karmaexchange.util.SearchUtil.getSearchableTokens;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class SearchUtilTest {

  @Test
  public void testGetSearchTokens() {
    assertEquals(
      ImmutableSet.of("amir", "test", "case", "isn't", "amazing", "instructive", "bus"),
      getSearchableTokens(
        "Amir's test case isn't \"amazing\" but it. Is 'instructive' bus buses able " +
        "token limit reached.", 7));
  }

}
