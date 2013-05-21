package org.karmaexchange.resources.msg;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.karmaexchange.util.URLUtil;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.collect.Maps;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
@XmlSeeAlso({EventParticipantView.class, EventSearchView.class})
public class ListResponseMsg<T> {

  private List<T> data;
  private PagingInfo paging;

  public static <T> ListResponseMsg<T> create(List<T> data) {
    return create(data, null);
  }

  public static <T> ListResponseMsg<T> create(List<T> data, @Nullable PagingInfo paging) {
    return new ListResponseMsg<T>(data, paging);
  }

  private ListResponseMsg(List<T> data, @Nullable PagingInfo paging) {
    this.data = data;
    this.paging = paging;
  }

  @Data
  @NoArgsConstructor
  public static class PagingInfo {
    public static final String AFTER_CURSOR_PARAM = "after";
    public static final String LIMIT_PARAM = "limit";
    public static final String OFFSET_PARAM = "offset";

    /**
     * The URL for the next set of results. If null, there are no more results.
     */
    @Nullable
    private String next;
    private String afterCursor;

    @Nullable
    public static PagingInfo create(@Nullable Cursor afterCursor, int limit, boolean moreResults,
                                    URI resourceUri, Map<String, Object> params) {
      String afterCursorStr = (afterCursor == null) ? "" : afterCursor.toWebSafeString();
      if (afterCursorStr.isEmpty()) {
        return null;
      } else {
        String nextUrl;
        if (moreResults) {
          Map<String, Object> finalParams = Maps.newHashMap(params);
          finalParams.put(AFTER_CURSOR_PARAM, afterCursorStr);
          finalParams.put(LIMIT_PARAM, limit);
          nextUrl = URLUtil.buildURL(resourceUri, finalParams);
        } else {
          nextUrl = null;
        }
        return new PagingInfo(nextUrl, afterCursorStr);
      }
    }

    @Nullable
    public static PagingInfo create(int currentOffset, int limit, int listSize,
                                    URI resourceUri, @Nullable Map<String, Object> params) {
      int nextOffset = currentOffset + limit;
      if (nextOffset >= listSize) {
        // No more results.
        return null;
      }
      Map<String, Object> finalParams =
          (params == null) ? Maps.<String, Object>newHashMap() : Maps.newHashMap(params);
      finalParams.put(OFFSET_PARAM, nextOffset);
      finalParams.put(LIMIT_PARAM, limit);
      String nextUrl = URLUtil.buildURL(resourceUri, finalParams);
      return new PagingInfo(nextUrl, null);
    }

    public static <T> List<T> createOffsettedResult(List<T> result, int offset, int limit) {
      if (offset >= result.size()) {
        return result.subList(0, 0);
      } else {
        limit = Math.min(limit, result.size() - offset);
        return result.subList(offset, offset + limit);
      }
    }

    private PagingInfo(@Nullable String nextUrl, @Nullable String afterCursor) {
      this.next = nextUrl;
      this.afterCursor = afterCursor;
    }
  }
}
