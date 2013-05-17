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

    /**
     * The URL for the next set of results. If null, there are no more results.
     */
    @Nullable
    private String next;
    private String afterCursor;

    @Nullable
    public static PagingInfo create(@Nullable Cursor afterCursor, int limit, int numResultsFetched,
                                    URI resourceUri, Map<String, Object> params) {
      if (afterCursor == null) {
        return null;
      } else {
        String afterCursorStr = afterCursor.toWebSafeString();
        String nextUrl;
        if (numResultsFetched == limit) {
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

    private PagingInfo(String nextUrl, String afterCursor) {
      this.next = nextUrl;
      this.afterCursor = afterCursor;
    }
  }
}
