package org.karmaexchange.util;

import static org.junit.Assert.*;
import static org.karmaexchange.util.SearchUtil.getSearchableTokens;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class SearchUtilTest {

  @Test
  public void testGetSearchTokens() {
    assertEquals(ImmutableSet.of("#tag"), getSearchableTokens("#tag", 10));
    assertEquals(
      ImmutableSet.of("amir", "test", "case", "isn't", "amazing", "instructive", "bus",
        "#tagsarealwaysextractedfirst"),
      getSearchableTokens(
        "Amir's test case isn't \"amazing\" but it. Is 'instructive' bus buses able " +
        "token limit reached. #tagsAreAlwaysExtractedFirst but #1 and #123baby are not tags", 8));
  }

}
