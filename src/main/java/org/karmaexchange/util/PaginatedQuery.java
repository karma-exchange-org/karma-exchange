package org.karmaexchange.util;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.appengine.api.datastore.Cursor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.cmd.Query;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginatedQuery<T> {

  private final Class<T> resourceClass;
  private final Collection<QueryClause> queryClauses;

  public Query<T> getOfyQuery() {
    Query<T> query = ofy().load().type(resourceClass);
    for (QueryClause queryClause : queryClauses) {
      query = queryClause.apply(query);
    }
    return query;
  }

  public Map<String, Object> getPaginationParams() {
    Map<String, Object> paginationParams = Maps.newHashMap();
    for (QueryClause queryClause : queryClauses) {
      PaginationParam param = queryClause.getPaginationParam();
      if (param != null) {
        paginationParams.put(param.getName(), param.getValue());
      }
    }
    return paginationParams;
  }

  @Data
  public static class Builder<T> {
    private final Class<T> resourceClass;
    private List<FilterQueryClause> queryFilters = Lists.newArrayList();
    @Nullable
    private AncestorQueryClause ancestorFilter;
    @Nullable
    private OrderQueryClause order;
    @Nullable
    private Integer limit;
    @Nullable
    private Cursor afterCursor;

    public static <T> Builder<T> create(Class<T> resourceClass) {
      return new Builder<T>(resourceClass);
    }

    private Builder(Class<T> resourceClass) {
      this.resourceClass = resourceClass;
    }

    public Builder<T> addFilter(FilterQueryClause filter) {
      queryFilters.add(filter);
      return this;
    }

    public Builder<T> addFilters(Collection<? extends FilterQueryClause> filters) {
      queryFilters.addAll(filters);
      return this;
    }

    public Builder<T> setAncestorFilter(@Nullable AncestorQueryClause ancestorFilter) {
      this.ancestorFilter = ancestorFilter;
      return this;
    }

    public Builder<T> setOrder(@Nullable OrderQueryClause order) {
      this.order = order;
      return this;
    }

    public Builder<T> setLimit(@Nullable Integer limit) {
      this.limit = limit;
      return this;
    }

    public Builder<T> setAfterCursor(@Nullable Cursor afterCursor) {
      this.afterCursor = afterCursor;
      return this;
    }

    public PaginatedQuery<T> build() {
      List<QueryClause> queryClauses = Lists.newArrayList();
      queryClauses.addAll(queryFilters);
      if (ancestorFilter != null) {
        queryClauses.add(ancestorFilter);
      }
      if (order != null) {
        queryClauses.add(order);
      }
      if (limit != null) {
        queryClauses.add(new LimitQueryClause(limit));
      }
      if (afterCursor != null) {
        queryClauses.add(new StartAtQueryClause(afterCursor));
      }
      return new PaginatedQuery<T>(resourceClass, queryClauses);
    }
  }

  @Data
  public static abstract class QueryClause {
    protected PaginationParam paginationParam;

    public abstract <T> Query<T> apply(Query<T> query);
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class FilterQueryClause extends QueryClause {

    private final String condition;
    private final Object value;

    public <T> Query<T> apply(Query<T> query) {
      return query.filter(condition, value);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class AncestorQueryClause extends QueryClause {

    private final Object ancestor;

    public <T> Query<T> apply(Query<T> query) {
      return query.ancestor(ancestor);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class OrderQueryClause extends QueryClause {

    private final String order;

    public <T> Query<T> apply(Query<T> query) {
      return query.order(order);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class LimitQueryClause extends QueryClause {

    private final int limit;

    public <T> Query<T> apply(Query<T> query) {
      return query.limit(limit);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  private static class StartAtQueryClause extends QueryClause {

    private final Cursor afterCursor;

    public <T> Query<T> apply(Query<T> query) {
      return query.startAt(afterCursor);
    }
  }
}
