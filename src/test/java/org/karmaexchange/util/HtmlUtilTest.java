package org.karmaexchange.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class HtmlUtilTest {

  @Test
  public void testToPlainTextNoHtml() {
    String plainTextStr = "Hello this isn't a complicated test. I hope this works!";
    assertEquals(plainTextStr, HtmlUtil.toPlainText(plainTextStr));
  }

  @Test
  public void testToPlainTextHtml1() {
    String htmlStr =
        "Hello this isn't a complicated test.<br> I hope this works!";
    String expectedTextStr =
        "Hello this isn't a complicated test.\n I hope this works!";
    assertEquals(expectedTextStr, HtmlUtil.toPlainText(htmlStr));
  }

  @Test
  public void testToPlainTextHtml2() {
    String htmlStr =
        "<p dir=\"ltr\" style=\"\">" +
        "<span id=\"docs-internal-guid-9049f8d3-d744-5029-84da-41d5cd6cd8a4\"><span style=\"font-size: 15px; font-family: Arial; \">Volunteers are a vital part of the Food Bank&#39;s efforts to help the 1 in 6 Alameda County residents we serve – including children, seniors and entire families. Our warehouse and office are abuzz with hunger relief activity, and your help is needed more than ever.</span></span></p>" +
        "<br><p dir=\"ltr\" style=\"\">" +
        "<span id=\"docs-internal-guid-9049f8d3-d744-5029-84da-41d5cd6cd8a4\"><span style=\"font-size: 15px; font-family: Arial; \">In 2013, more than 13,000 volunteers were instrumental in helping us distribute more than 2 million pounds of food per month!</span></span></p>" +
        "<br><p dir=\"ltr\" style=\"\">" +
        "<span id=\"docs-internal-guid-9049f8d3-d744-5029-84da-41d5cd6cd8a4\"><span style=\"font-size: 15px; font-family: Arial; \">Individuals and groups are needed Monday - Friday to help sort, screen and box fresh produce and non-perishable food for distribution to the Food Bank’s partner agencies. (click to see more)</span></span></p>" +
        "<div>" +
        " </div>";
    String expectedTextStr =
        "Volunteers are a vital part of the Food Bank's efforts to help the 1 in 6 Alameda County residents we serve – including children, seniors and entire families. Our warehouse and office are abuzz with hunger relief activity, and your help is needed more than ever.\n\n" +
        "In 2013, more than 13,000 volunteers were instrumental in helping us distribute more than 2 million pounds of food per month!\n\n" +
        "Individuals and groups are needed Monday - Friday to help sort, screen and box fresh produce and non-perishable food for distribution to the Food Bank’s partner agencies. (click to see more)";
    assertEquals(expectedTextStr, HtmlUtil.toPlainText(htmlStr).trim());
  }

}
