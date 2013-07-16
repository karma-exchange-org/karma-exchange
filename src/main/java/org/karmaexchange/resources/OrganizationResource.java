package org.karmaexchange.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginationParam;
import org.karmaexchange.util.PaginatedQuery.StartsWithFilter;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.cmd.Query;

@Path("/org")
public class OrganizationResource extends BaseDaoResource<Organization> {

  public static final String NAME_PREFIX_PARAM = "name_prefix";

  @Override
  protected Class<Organization> getResourceClass() {
    return Organization.class;
  }

  @Override
  protected void preProcessUpsert(Organization org) {
    if (!org.isKeyComplete()) {
      org.initFromPage();
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResources() {
    MultivaluedMap<String, String> queryParams =
        uriInfo.getQueryParameters();
    String afterCursorStr =
        queryParams.getFirst(PagingInfo.AFTER_CURSOR_PARAM);
    int limit = Integer.valueOf(queryParams.containsKey(PagingInfo.LIMIT_PARAM) ?
        queryParams.getFirst(PagingInfo.LIMIT_PARAM) : DEFAULT_NUM_SEARCH_RESULTS);
    String namePrefix = queryParams.getFirst(NAME_PREFIX_PARAM);

    PaginatedQuery.Builder<Organization> queryBuilder =
        PaginatedQuery.Builder.create(Organization.class);
    if (afterCursorStr != null) {
      queryBuilder.setAfterCursor(Cursor.fromWebSafeString(afterCursorStr));
    }
    if (namePrefix != null) {
      queryBuilder.addFilter(new NamePrefixFilter(namePrefix.toLowerCase()));
    }
    // Query one more than the limit to see if we need to provide a link to additional results.
    queryBuilder.setLimit(limit + 1);

    PaginatedQuery<Organization> paginatedQuery = queryBuilder.build();
    Query<Organization> ofyQuery = paginatedQuery.getOfyQuery();

    QueryResultIterator<Organization> queryIter = ofyQuery.iterator();
    List<Organization> searchResults = Lists.newArrayList(Iterators.limit(queryIter, limit));
    Cursor afterCursor = queryIter.getCursor();
    BaseDao.processLoadResults(searchResults);

    ListResponseMsg<Organization> orgs = ListResponseMsg.create(
      searchResults,
      PagingInfo.create(afterCursor, limit, queryIter.hasNext(), uriInfo.getAbsolutePath(),
        paginatedQuery.getPaginationParams()));
    return Response.ok(new GenericEntity<ListResponseMsg<Organization>>(orgs) {}).build();
  }

  private static class NamePrefixFilter extends StartsWithFilter {
    public NamePrefixFilter(String namePrefix) {
      super("searchableOrgName", namePrefix.toLowerCase());
      paginationParam = new PaginationParam(NAME_PREFIX_PARAM, namePrefix);
    }
  }
}
