package org.karmaexchange.util;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.cmd.Query;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginatedQuery<T extends BaseDao<T>> {

  private final Class<T> resourceClass;
  private final Collection<QueryClause> queryClauses;
  private final UriInfo uriInfo;
  private final int limit;

  public Result<T> execute() {
    QueryResultIterator<T> queryIter = getOfyQuery().iterator();
    List<T> searchResults = Lists.newArrayList(Iterators.limit(queryIter, limit));
    Cursor afterCursor = queryIter.getCursor();
    BaseDao.processLoadResults(searchResults);
    PagingInfo pagingInfo = PagingInfo.create(afterCursor, limit, queryIter.hasNext(), uriInfo);
    return new Result<T>(searchResults, pagingInfo);
  }

  private Query<T> getOfyQuery() {
    Query<T> query = ofy().load().type(resourceClass);
    for (QueryClause queryClause : queryClauses) {
      query = queryClause.apply(query);
    }
    return query;
  }

  @Data
  public static class Result<T extends BaseDao<T>> {
    private final List<T> searchResults;
    private final PagingInfo pagingInfo;
  }

  @Data
  public static class Builder<T extends BaseDao<T>> {
    private final Class<T> resourceClass;
    private List<FilterQueryClause> queryFilters = Lists.newArrayList();
    @Nullable
    private Object ancestor;
    @Nullable
    private String order;
    private final int limit;
    @Nullable
    private final Cursor afterCursor;
    private final UriInfo uriInfo;

    public static <T extends BaseDao<T>> Builder<T> create(Class<T> resourceClass,
        UriInfo uriInfo, int defaultLimit) {
      return new Builder<T>(resourceClass, uriInfo, defaultLimit);
    }

    private Builder(Class<T> resourceClass, UriInfo uriInfo, int defaultLimit) {
      this.resourceClass = resourceClass;
      this.uriInfo = uriInfo;
      MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
      String afterCursorStr = queryParams.getFirst(PagingInfo.AFTER_CURSOR_PARAM);
      if (afterCursorStr != null) {
        afterCursor = Cursor.fromWebSafeString(afterCursorStr);
      } else {
        afterCursor = null;
      }
      limit = queryParams.containsKey(PagingInfo.LIMIT_PARAM) ?
          Integer.valueOf(queryParams.getFirst(PagingInfo.LIMIT_PARAM)) : defaultLimit;
      if (limit <= 0) {
        throw ErrorResponseMsg.createException("limit must be greater than zero",
          ErrorInfo.Type.BAD_REQUEST);
      }
    }

    public Builder<T> addFilter(FilterQueryClause filter) {
      queryFilters.add(filter);
      return this;
    }

    public Builder<T> addFilters(Collection<? extends FilterQueryClause> filters) {
      queryFilters.addAll(filters);
      return this;
    }

    public Builder<T> setAncestor(Object ancestor) {
      this.ancestor = ancestor;
      return this;
    }

    public Builder<T> setOrder(String order) {
      this.order = order;
      return this;
    }

    public PaginatedQuery<T> build() {
      List<QueryClause> queryClauses = Lists.newArrayList();
      queryClauses.addAll(queryFilters);
      if (ancestor != null) {
        queryClauses.add(new AncestorQueryClause(ancestor));
      }
      if (order != null) {
        queryClauses.add(new OrderQueryClause(order));
      }
      // Request for one more than the limit to determine if there are results after the limit.
      queryClauses.add(new LimitQueryClause(limit + 1));
      if (afterCursor != null) {
        queryClauses.add(new StartAtQueryClause(afterCursor));
      }
      return new PaginatedQuery<T>(resourceClass, queryClauses, uriInfo, limit);
    }
  }

  @Data
  public static abstract class QueryClause {
     public abstract <T> Query<T> apply(Query<T> query);
  }

  public static abstract class FilterQueryClause extends QueryClause {
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class ConditionFilter extends FilterQueryClause {

    private final String condition;
    private final Object[] values;

    public ConditionFilter(String condition, Object... values) {
      this.condition = condition;
      this.values = values;
    }

    @Override
    public <T> Query<T> apply(Query<T> query) {
      for (Object value : values) {
        query = query.filter(condition, value);
      }
      return query;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class StartsWithFilter extends FilterQueryClause {

    private final String fieldName;
    private final String prefix;

    @Override
    public <T> Query<T> apply(Query<T> query) {
      return query.filter(fieldName + " >=", prefix)
          .filter(fieldName + " <", prefix + "\uFFFD");
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class AncestorQueryClause extends QueryClause {

    private final Object ancestor;

    @Override
    public <T> Query<T> apply(Query<T> query) {
      return query.ancestor(ancestor);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class OrderQueryClause extends QueryClause {

    private final String order;

    @Override
    public <T> Query<T> apply(Query<T> query) {
      return query.order(order);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class LimitQueryClause extends QueryClause {

    private final int limit;

    @Override
    public <T> Query<T> apply(Query<T> query) {
      return query.limit(limit);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class StartAtQueryClause extends QueryClause {

    private final Cursor afterCursor;

    @Override
    public <T> Query<T> apply(Query<T> query) {
      return query.startAt(afterCursor);
    }
  }
}
