package org.karmaexchange.util;

import java.util.regex.Pattern;

public class TagUtil {

  public static final String TAG_PREFIX = "#";

  public static final Pattern TAG_PREFIX_PATTERN = Pattern.compile("\\A#[a-zA-Z]");

  public static String createTag(String text) {
    String tagName = text.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    // We're preventing strings like #1 from being parsed as tags.
    if (tagName.isEmpty() || Character.isDigit(tagName.charAt(0))) {
      throw new IllegalArgumentException("Unable to create tag for string: '" + text + "'");
    }
    return TAG_PREFIX + tagName;
  }
}
