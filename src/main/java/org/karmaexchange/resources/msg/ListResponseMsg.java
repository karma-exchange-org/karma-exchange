package org.karmaexchange.resources.msg;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserManagedEvent;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.URLUtil;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
@XmlSeeAlso({EventParticipantView.class, EventSearchView.class, ReviewCommentView.class,
  Organization.class, OrganizationMemberView.class, OrganizationMembershipView.class,
  WaiverSummaryView.class, UserManagedEvent.class, User.class})
public class ListResponseMsg<T> {

  private List<T> data;
  private PagingInfo paging;

  public static <T> ListResponseMsg<T> create(List<T> data) {
    return create(data, null);
  }

  public static <T extends BaseDao<T>> ListResponseMsg<T> create(
      PaginatedQuery.Result<T> queryResult) {
    return create(queryResult.getSearchResults(), queryResult.getPagingInfo());
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
        UriInfo uriInfo) {
      String afterCursorStr = (afterCursor == null) ? "" : afterCursor.toWebSafeString();
      if (afterCursorStr.isEmpty()) {
        return null;
      } else {
        String nextUrl;
        if (moreResults) {
          Multimap<String, String> queryParams = toMultimap(uriInfo.getQueryParameters());
          queryParams.replaceValues(AFTER_CURSOR_PARAM, asList(afterCursorStr));
          queryParams.replaceValues(LIMIT_PARAM, asList(String.valueOf(limit)));
          nextUrl = URLUtil.buildURL(uriInfo.getAbsolutePath(), queryParams);
        } else {
          nextUrl = null;
        }
        return new PagingInfo(nextUrl, afterCursorStr);
      }
    }

    @Nullable
    public static PagingInfo create(int listSize, UriInfo uriInfo, int defaultLimit) {
      MultivaluedMap<String, String> inputQueryParams = uriInfo.getQueryParameters();
      int currentOffset = inputQueryParams.containsKey(OFFSET_PARAM) ?
          Integer.valueOf(inputQueryParams.getFirst(OFFSET_PARAM)) : 0;
      int limit = inputQueryParams.containsKey(LIMIT_PARAM) ?
          Integer.valueOf(inputQueryParams.getFirst(LIMIT_PARAM)) : defaultLimit;
      int nextOffset = currentOffset + limit;
      if (nextOffset >= listSize) {
        // No more results.
        return null;
      }
      Multimap<String, String> outputQueryParams = toMultimap(uriInfo.getQueryParameters());
      outputQueryParams.replaceValues(OFFSET_PARAM, asList(String.valueOf(nextOffset)));
      outputQueryParams.replaceValues(LIMIT_PARAM, asList(String.valueOf(limit)));
      String nextUrl = URLUtil.buildURL(uriInfo.getAbsolutePath(), outputQueryParams);
      return new PagingInfo(nextUrl, null);
    }

    public static <T> List<T> offsetResult(List<T> result, UriInfo uriInfo, int defaultLimit) {
      MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
      int offset = queryParams.containsKey(OFFSET_PARAM) ?
          Integer.valueOf(queryParams.getFirst(OFFSET_PARAM)) : 0;
      int limit = queryParams.containsKey(LIMIT_PARAM) ?
          Integer.valueOf(queryParams.getFirst(LIMIT_PARAM)) : defaultLimit;
      validateOffsettedResultParams(offset, limit);
      if (offset >= result.size()) {
        return result.subList(0, 0);
      } else {
        limit = Math.min(limit, result.size() - offset);
        return result.subList(offset, offset + limit);
      }
    }

    private static void validateOffsettedResultParams(int offset, int limit) {
      if (limit <= 0) {
        throw ErrorResponseMsg.createException("limit must be greater than zero",
          ErrorInfo.Type.BAD_REQUEST);
      }
      if (offset < 0) {
        throw ErrorResponseMsg.createException("offset must be greater than or equal to zero",
          ErrorInfo.Type.BAD_REQUEST);
      }
    }

    private static Multimap<String, String> toMultimap(
        MultivaluedMap<String, String> multivaluedMap) {
      Multimap<String, String> multimap = ArrayListMultimap.create();
      for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
        multimap.putAll(entry.getKey(), entry.getValue());
      }
      return multimap;
    }

    private PagingInfo(@Nullable String nextUrl, @Nullable String afterCursor) {
      this.next = nextUrl;
      this.afterCursor = afterCursor;
    }
  }
}
