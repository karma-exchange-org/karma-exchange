package org.karmaexchange.util;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * HTML utility functions.
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 * @author Amir Valiani
 */
public class HtmlUtil {

  /**
   * Format an HTML string to plain-text.
   *
   * @param htmlStr a string containing HTML markup
   * @return formatted text
   */
  public static String toPlainText(String htmlStr) {
    Document doc = Jsoup.parse(htmlStr);
    PlainTextFormattingVisitor formatter = new PlainTextFormattingVisitor();
    NodeTraversor traversor = new NodeTraversor(formatter);
    traversor.traverse(doc); // walk the DOM, and call .head() and .tail() for each node
    return formatter.toString();
  }

  // the formatting rules, implemented in a breadth-first DOM traverse
  private static class PlainTextFormattingVisitor implements NodeVisitor {
    private StringBuilder accum = new StringBuilder(); // holds the accumulated text

    // hit when the node is first seen
    public void head(Node node, int depth) {
      String name = node.nodeName();
      if (node instanceof TextNode)
        append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
      else if (name.equals("li")) append("\n * ");
    }

    // hit when all of the node's children (if any) have been visited
    public void tail(Node node, int depth) {
      String name = node.nodeName();
      if (name.equals("br"))
        append("\n");
      else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5"))
        append("\n");
    }

    // appends text to the string builder with a simple word wrap method
    private void append(String text) {
      if (text.equals(" ") &&
          ( (accum.length() == 0) ||
             StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
        return; // don't accumulate long runs of empty spaces

      accum.append(text);
    }

    public String toString() {
      return accum.toString();
    }
  }
}
