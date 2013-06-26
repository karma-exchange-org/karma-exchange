package org.karmaexchange.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TagUtilTest {

  @Test
  public void testCreateTag() {
    assertEquals("#thisisawesome408", TagUtil.createTag("This is-awesome' \"408\"!!!"));
    verifyTagIsInvalid("1");
    verifyTagIsInvalid("");
  }

  private void verifyTagIsInvalid(String tag) {
    try {
      TagUtil.createTag(tag);
      fail("Exception not generated");
    } catch (IllegalArgumentException e) {
      assertEquals(e.getMessage(), "Unable to create tag for string: '" + tag + "'");
    }
  }

}
