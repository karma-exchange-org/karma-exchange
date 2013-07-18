package org.karmaexchange.resources;

import static com.google.common.base.Preconditions.checkState;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMemberView;
import org.karmaexchange.resources.msg.ListResponseMsg.PagingInfo;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;
import org.karmaexchange.util.PaginatedQuery.OrderQueryClause;
import org.karmaexchange.util.PaginatedQuery.StartsWithFilter;
import org.karmaexchange.util.PaginationParam;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

@Path("/org")
public class OrganizationResource extends BaseDaoResource<Organization> {

  public static final String NAME_PREFIX_PARAM = "name_prefix";
  public static final String ROLE_PARAM = "role";

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
    queryBuilder.setOrder(new OrderQueryClause("searchableOrgName"));
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

  @Path("{org}/member")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMemberView> getMember(
      @PathParam("org") String orgKeyStr,
      @QueryParam(ROLE_PARAM) Organization.Role role,
      @QueryParam(PagingInfo.AFTER_CURSOR_PARAM) String afterCursorStr,
      @QueryParam(PagingInfo.LIMIT_PARAM) @DefaultValue(DEFAULT_NUM_SEARCH_RESULTS) int limit) {
    Key<Organization> orgKey = Key.<Organization>create(orgKeyStr);
    PaginatedQuery.Builder<User> queryBuilder =
        PaginatedQuery.Builder.create(User.class);
    if (afterCursorStr != null) {
      queryBuilder.setAfterCursor(Cursor.fromWebSafeString(afterCursorStr));
    }
    String orgCondition;
    if ((role == null) || (role == Organization.Role.MEMBER)) {
      orgCondition = "organizationMemberships.organization.key";
    } else if (role == Organization.Role.ADMIN) {
      orgCondition = "organizationMemberships.organizationWithAdminRole.key";
    } else {
      checkState(role == Organization.Role.ORGANIZER);
      orgCondition = "organizationMemberships.organizationWithOrganizerRole.key";
    }
    queryBuilder.addFilter(new RoleFilter(role, orgCondition, orgKey));
    queryBuilder.setOrder(new OrderQueryClause("searchableFullName"));
    // Query one more than the limit to see if we need to provide a link to additional results.
    queryBuilder.setLimit(limit + 1);

    PaginatedQuery<User> paginatedQuery = queryBuilder.build();
    Query<User> ofyQuery = paginatedQuery.getOfyQuery();

    QueryResultIterator<User> queryIter = ofyQuery.iterator();
    List<User> searchResults = Lists.newArrayList(Iterators.limit(queryIter, limit));
    Cursor afterCursor = queryIter.getCursor();
    BaseDao.processLoadResults(searchResults);

    // TOOD(avlaiani): auto-propogate query params

    return ListResponseMsg.create(
      OrganizationMemberView.create(searchResults, orgKey),
      PagingInfo.create(afterCursor, limit, queryIter.hasNext(), uriInfo.getAbsolutePath(),
        paginatedQuery.getPaginationParams()));
  }

  private static class RoleFilter extends ConditionFilter {
    public RoleFilter(Organization.Role role, String condition, Key<Organization> orgKey) {
      super(condition, orgKey);
      if (role != null) {
        paginationParam = new PaginationParam(ROLE_PARAM, role.toString());
      }
    }
  }

  @Path("{org}/member")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public void upsertMember(
      @PathParam("org") String orgKeyStr,
      @QueryParam("user") String userKeyStr,
      @QueryParam(ROLE_PARAM) Organization.Role role) {
    Key<Organization> orgKey = Key.<Organization>create(orgKeyStr);
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    if (role == null) {
      role = Organization.Role.MEMBER;
    }
    User.updateMembership(userKey, orgKey, role);
  }

  @Path("{org}/member")
  @DELETE
  public void deleteMember(
      @PathParam("org") String orgKeyStr,
      @QueryParam("user") String userKeyStr) {
    Key<Organization> orgKey = Key.<Organization>create(orgKeyStr);
    Key<User> userKey = (userKeyStr == null) ? getCurrentUserKey() : Key.<User>create(userKeyStr);
    User.updateMembership(userKey, orgKey, null);
  }
}
