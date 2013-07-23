package org.karmaexchange.resources;

import static com.google.common.base.Preconditions.checkState;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.RequestStatus;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMemberView;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;
import org.karmaexchange.util.PaginatedQuery.StartsWithFilter;

import com.googlecode.objectify.Key;

@Path("/org")
public class OrganizationResource extends BaseDaoResource<Organization> {

  public static final String NAME_PREFIX_PARAM = "name_prefix";
  public static final String ROLE_PARAM = "role";
  public static final String MEMBERSHIP_STATUS_PARAM = "membership_status";

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
    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
    String namePrefix = queryParams.getFirst(NAME_PREFIX_PARAM);

    PaginatedQuery.Builder<Organization> queryBuilder =
        PaginatedQuery.Builder.create(Organization.class, uriInfo, DEFAULT_NUM_SEARCH_RESULTS)
        .setOrder("searchableOrgName");
    if (namePrefix != null) {
      queryBuilder.addFilter(new StartsWithFilter("searchableOrgName", namePrefix.toLowerCase()));
    }

    ListResponseMsg<Organization> orgs = ListResponseMsg.create(queryBuilder.build().execute());
    return Response.ok(new GenericEntity<ListResponseMsg<Organization>>(orgs) {}).build();
  }

  @Path("{org}/member")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMemberView> getMembers(
      @PathParam("org") String orgKeyStr,
      @QueryParam(ROLE_PARAM) Organization.Role role,
      @QueryParam(MEMBERSHIP_STATUS_PARAM) RequestStatus membershipStatus) {
    Key<Organization> orgKey = Key.<Organization>create(orgKeyStr);
    String membershipCondition;
    if (membershipStatus == RequestStatus.PENDING) {
      if (role != null) {
        throw ErrorResponseMsg.createException(
          "role can not be specified for PENDING membership requests",
          ErrorInfo.Type.BAD_REQUEST);
      }
      membershipCondition = "organizationMemberships.organizationPendingMembershipRequest.key";
    } else {
      if (role == null) {
        role = Organization.Role.MEMBER;
      }
      if (role == Organization.Role.MEMBER) {
        membershipCondition = "organizationMemberships.organizationMember.key";
      } else if (role == Organization.Role.ADMIN) {
        membershipCondition = "organizationMemberships.organizationMemberWithAdminRole.key";
      } else {
        checkState(role == Organization.Role.ORGANIZER);
        membershipCondition = "organizationMemberships.organizationMemberWithOrganizerRole.key";
      }
    }
    PaginatedQuery.Builder<User> queryBuilder =
        PaginatedQuery.Builder.create(User.class, uriInfo, DEFAULT_NUM_SEARCH_RESULTS)
        .addFilter(new ConditionFilter(membershipCondition, orgKey))
        .setOrder("searchableFullName");
    PaginatedQuery.Result<User> queryResult = queryBuilder.build().execute();
    return ListResponseMsg.create(
      OrganizationMemberView.create(queryResult.getSearchResults(), orgKey),
      queryResult.getPagingInfo());
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
