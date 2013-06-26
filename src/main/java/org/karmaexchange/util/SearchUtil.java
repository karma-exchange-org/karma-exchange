package org.karmaexchange.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import com.google.common.collect.ImmutableSet;

public class SearchUtil {

  private static final Analyzer ANALYZER = new KStemEnglishAnalyzer();

  public static Set<String> getSearchableTokens(String textToParse, int maxTokens) {
    BoundedHashSet<String> searchableTokens = BoundedHashSet.create(maxTokens);
    addSearchableTokens(searchableTokens, textToParse);
    return searchableTokens;
  }

  public static void addSearchableTokens(BoundedHashSet<String> searchableTokens,
      String textToParse) {
    textToParse = extractTags(searchableTokens, textToParse);
    try {
      TokenStream tokenStream  = ANALYZER.tokenStream(null, new StringReader(textToParse));
      CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
      try {
        tokenStream.reset();
        while (!searchableTokens.limitReached() && tokenStream.incrementToken()) {
          searchableTokens.add(termAttr.toString());
        }
        tokenStream.end();
      } finally {
        tokenStream.close();
      }
    } catch (IOException e) {
      // Should not be thrown since a string is the input.
      throw new RuntimeException(e);
    }
  }

  private static String extractTags(BoundedHashSet<String> searchableTokens, String textToParse) {
    String[] tokens = textToParse.split("\\s+");
    StringBuilder remainingText = new StringBuilder();
    for (int tokIdx=0; (tokIdx < tokens.length) && !searchableTokens.limitReached();
         tokIdx++) {
      if (TagUtil.TAG_PREFIX_PATTERN.matcher(tokens[tokIdx]).find()) {
        searchableTokens.add(tokens[tokIdx].toLowerCase());
      } else {
        remainingText.append(tokens[tokIdx]);
        remainingText.append(' ');
      }
    }
    return remainingText.toString();
  }

  /**
   * This class is a combination of the StandardAnalyzer and the EnglishAnalyzer modified to
   * use the KStemFilter and a larger stop word list.
   */
  // TODO(avaliani): Consider adding a filter to handle contractions like isn't.
  private static final class KStemEnglishAnalyzer extends StopwordAnalyzerBase {

    /** Default maximum allowed token length */
    private static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

    private static final Version LUCENE_VERSION = Version.LUCENE_43;
    private static final CharArraySet STOP_WORDS;
    static {
      // This list is larger than the included stop word list in lucene.
      // Source: http://www.textfixer.com/resources/common-english-words.txt
      String stopWords = "a,able,about,across,after,all,almost,also,am,among,an,and,any,are,as,at,be,because,been,but,by,can,cannot,could,dear,did,do,does,either,else,ever,every,for,from,get,got,had,has,have,he,her,hers,him,his,how,however,i,if,in,into,is,it,its,just,least,let,like,likely,may,me,might,most,must,my,neither,no,nor,not,of,off,often,on,only,or,other,our,own,rather,said,say,says,she,should,since,so,some,than,that,the,their,them,then,there,these,they,this,tis,to,too,twas,us,wants,was,we,were,what,when,where,which,while,who,whom,why,will,with,would,yet,you,your";
      STOP_WORDS =
          new CharArraySet(LUCENE_VERSION, ImmutableSet.copyOf(stopWords.split(",")), true);
    }

    public KStemEnglishAnalyzer() {
      super(LUCENE_VERSION, STOP_WORDS);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      final StandardTokenizer src = new StandardTokenizer(matchVersion, reader);
      src.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);
      TokenStream result = new StandardFilter(matchVersion, src);
      result = new EnglishPossessiveFilter(matchVersion, result);
      result = new LowerCaseFilter(matchVersion, result);
      result = new StopFilter(matchVersion, result, stopwords);
      result = new KStemFilter(result);
      // result = new PorterStemFilter(result);
      return new TokenStreamComponents(src, result) {
        @Override
        protected void setReader(final Reader reader) throws IOException {
          src.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);
          super.setReader(reader);
        }
      };
    }
  }
}
