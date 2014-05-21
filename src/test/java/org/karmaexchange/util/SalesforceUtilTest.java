package org.karmaexchange.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.karmaexchange.dao.derived.SourceEventGeneratorInfo;

public class SalesforceUtilTest {

  @Test
  public void testProcessRichTextField() {
    String rtfContent = "<strong>Let&#39;s try and do some rtf.</strong><br><br>Image 1:<br><br><img alt=\"Anon User Image\" src=\"https://c.na15.content.force.com/servlet/rtaImage?eid=a02i000000FODHi&amp;feoid=00Ni000000DUtH7&amp;refid=0EMi00000008fan\"></img><br><br>Let&#39;s see how links are done as well.<br><br>And bullets" +
        "<ul><li>" +
        "     one bullet</li><li>" +
        "     two bullets</li><li>" +
        "     three bullets</li></ul>" +
        "Indents" +
        "<div style=\"margin-left: 40px; \">" +
        " Indented 1</div>" +
        "<div style=\"margin-left: 80px; \">" +
        " Indented 2</div>" +
        "<br>Image with proper url: " +
        "<div style=\"text-align: center; \">" +
        " <img alt=\"karma logo\" src=\"https://karmademo.appspot.com/img/logo.png\"></img></div>" +
        "<br><br>";

    SourceEventGeneratorInfo sourceInfo =
        new SourceEventGeneratorInfo(null, "x", "kex-developer-edition.na15.force.com");

    String processedRtfContent = SalesforceUtil.processRichTextField(rtfContent, sourceInfo);

    assertTrue("processed field should not be wrapped in an html tag",
      !processedRtfContent.toLowerCase().contains("<html>"));
    assertTrue("processed field should not be wrapped in an body tag",
      !processedRtfContent.toLowerCase().contains("<body>"));
    assertTrue("salesforce images should be in the unprocessed content",
      rtfContent.contains(SalesforceUtil.IMG_CDN_DOMAIN));
    assertTrue("salesforce images should not be directly referenced",
      !processedRtfContent.contains(SalesforceUtil.IMG_CDN_DOMAIN));
    assertTrue("salesforce images should be re-route to the public servlet",
      processedRtfContent.contains("https://kex-developer-edition.na15.force.com/servlet/rtaImage?eid=a02i000000FODHi&amp;feoid=00Ni000000DUtH7&amp;refid=0EMi00000008fan"));
    assertTrue("content 'try and do some rtf' must be present",
      processedRtfContent.contains("try and do some rtf"));
    assertTrue("content 'one bullet' must be present",
      processedRtfContent.contains("one bullet"));
  }

}
