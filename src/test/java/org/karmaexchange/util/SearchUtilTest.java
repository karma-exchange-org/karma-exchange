package org.karmaexchange.util;

import static org.junit.Assert.*;
import static org.karmaexchange.util.SearchUtil.getSearchableTokens;
import static org.karmaexchange.util.SearchUtil.addSearchableTokens;

import java.util.EnumSet;

import org.junit.Test;
import org.karmaexchange.util.SearchUtil.ParseOptions;
import org.karmaexchange.util.SearchUtil.ReservedToken;

import com.google.common.collect.ImmutableSet;

public class SearchUtilTest {

  @Test
  public void testGetSearchableTokens() {
    assertEquals(ImmutableSet.of("#tag"), getSearchableTokens("#tag", 10));
    assertEquals(
      ImmutableSet.of("amir", "test", "case", "isn't", "amazing", "instructive", "bus",
        "#tagsarealwaysextractedfirst"),
      getSearchableTokens(
        "Amir's test case isn't \"amazing\" but it. Is 'instructive' bus buses able " +
        "token limit reached. #tagsAreAlwaysExtractedFirst but #1 and #123baby are not tags", 8));

    assertEquals(ImmutableSet.of("org:xyz"),
      getSearchableTokens(SearchUtil.ReservedToken.ORG.create("xyz"), 10));
  }

  @Test
  public void testAddSearchableTokens() {
    for (ReservedToken reservedToken : ReservedToken.values()) {
      BoundedHashSet<String> tokenSet = BoundedHashSet.create(10);
      addSearchableTokens(tokenSet, reservedToken.create("xyz"),
        EnumSet.noneOf(ParseOptions.class));
      assertEquals(ImmutableSet.of(reservedToken.create("xyz")), tokenSet);

      tokenSet = BoundedHashSet.create(10);
      addSearchableTokens(tokenSet, reservedToken.create("xyz"),
        EnumSet.of(ParseOptions.EXCLUDE_RESERVED_TOKENS));
      assertEquals(ImmutableSet.of(), tokenSet);
    }
  }
}
