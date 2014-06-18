package org.karmaexchange.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.karmaexchange.dao.derived.EventSourceInfo;

public class SalesforceUtil {

  public static final String REGISTRATION_API_PATH = "/services/apexrest/registration";

  static final String IMG_CDN_DOMAIN = "content.force.com";

  public static String processRichTextField(String htmlStr, EventSourceInfo sourceInfo) {
    Document doc = Jsoup.parseBodyFragment(htmlStr);

    // For now we'll just strip images that are stored in the salesforce db and retain
    // the remainder of the markup.

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

    Elements elsWithStyle = doc.getElementsByAttribute("style");
    for (Element elWithStyle : elsWithStyle) {
      String inputStyleAttr = elWithStyle.attr("style");
      StringBuilder outputStyleAttr = new StringBuilder();
      String[] cssProperties = inputStyleAttr.split(";");
      for (String cssProperty : cssProperties) {
        String cssPropertyName = cssProperty.split(":", 2)[0];
        cssPropertyName = cssPropertyName.trim();
        if (!cssPropertyName.equalsIgnoreCase("font-size") &&
            !cssPropertyName.equalsIgnoreCase("font-family") ) {
          if (outputStyleAttr.length() != 0) {
            outputStyleAttr.append(";");
          }
          outputStyleAttr.append(cssPropertyName);
        }
      }
      elWithStyle.attr("style", outputStyleAttr.toString());
    }

    return doc.body().html().toString();
  }

}
