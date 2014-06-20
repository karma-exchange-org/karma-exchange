package org.karmaexchange.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.karmaexchange.dao.derived.EventSourceInfo;

public class SalesforceUtil {

  public static final String REGISTRATION_API_PATH = "/services/apexrest/registration";

  static final String IMG_CDN_DOMAIN = "content.force.com";

  private static final List<String> RESTRICTED_CSS_PROPERTY_NAMES =
      Arrays.asList("font-size", "font-family", "background-color");

  public static String processRichTextField(String htmlStr, EventSourceInfo sourceInfo) {
    Document doc = Jsoup.parseBodyFragment(htmlStr);

    updateSalesforceCdnImgLinks(doc, sourceInfo);
    removeRestrictedCssProperties(doc);

    return doc.body().html().toString();
  }

  private static void updateSalesforceCdnImgLinks(Document doc, EventSourceInfo sourceInfo) {
    Elements imgs = doc.getElementsByTag("img");
    for (Element img : imgs) {
      URI uri = null;
      try {
        uri = new URI(img.attr("src"));
      } catch (URISyntaxException e) {
        // Ignore bad uri
      }
      if (uri != null) {
        String domain = uri.getHost();
        if (domain.toLowerCase().endsWith(IMG_CDN_DOMAIN)) {
          img.attr("src",
            "https://" + sourceInfo.getDomain() + uri.getPath() + "?" + uri.getQuery());
        }
      }
    }
  }

  private static void removeRestrictedCssProperties(Document doc) {
    Elements elsWithStyle = doc.getElementsByAttribute("style");
    for (Element elWithStyle : elsWithStyle) {
      String inputStyleAttr =
          elWithStyle.attr("style");
      StringBuilder outputStyleAttr =
          new StringBuilder();
      String[] cssProperties =
          inputStyleAttr.split(";");
      for (String cssProperty : cssProperties) {
        String cssPropertyName = cssProperty.split(":", 2)[0];
        cssPropertyName = cssPropertyName.trim();
        // We don't let the user adjust the font-size or the font-family since we control
        // that in the UI.
        if (!cssPropertyName.isEmpty() &&
            !RESTRICTED_CSS_PROPERTY_NAMES.contains(cssPropertyName) ) {
          if (outputStyleAttr.length() != 0) {
            outputStyleAttr.append(";");
          }
          outputStyleAttr.append(cssProperty);
        }
      }
      if (outputStyleAttr.length() > 0) {
        elWithStyle.attr("style", outputStyleAttr.toString());
      } else {
        elWithStyle.removeAttr("style");
      }
    }
  }

}
