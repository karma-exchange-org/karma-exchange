package org.karmaexchange.resources;

import static com.google.common.base.Preconditions.checkState;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.net.URI;
import java.util.Collections;
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.karmaexchange.dao.Leaderboard;
import org.karmaexchange.dao.Leaderboard.LeaderboardType;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.RequestStatus;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.Waiver;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMemberView;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.WaiverSummaryView;
import org.karmaexchange.util.OfyUtil;
import org.karmaexchange.util.PaginatedQuery;
import org.karmaexchange.util.PaginatedQuery.ConditionFilter;
import org.karmaexchange.util.PaginatedQuery.StartsWithFilter;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@Path("/org")
public class OrganizationResource extends BaseDaoResource<Organization> {

  public static final String NAME_PREFIX_PARAM = "name_prefix";
  public static final String ROLE_PARAM = "role";
  public static final String MEMBERSHIP_STATUS_PARAM = "membership_status";
  public static final String LEADERBOARD_TYPE_PARAM = "type";
  public static final String INCLUDE_PARENT_ORGS_PARAM = "include_parent_orgs";

  @Override
  protected Class<Organization> getResourceClass() {
    return Organization.class;
  }

  @Override
  protected void preProcessUpsert(Organization org) {
    if (!org.isKeyComplete()) {
      org.initFromPage(servletContext, uriInfo.getRequestUri());
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

  @Path("{org}/children")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<Organization> getChildren(
      @PathParam("org") String orgKeyStr) {
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
    List<Organization> childOrgs =
        ofy().load().type(Organization.class).filter("parentOrg.key", orgKey).list();
    Collections.sort(childOrgs, Organization.OrgNameComparator.INSTANCE);
    return ListResponseMsg.create(childOrgs);
  }

  @Path("{org}/member")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMemberView> getMembers(
      @PathParam("org") String orgKeyStr,
      @QueryParam(ROLE_PARAM) Organization.Role role,
      @QueryParam(MEMBERSHIP_STATUS_PARAM) RequestStatus membershipStatus) {
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
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
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
    Key<User> userKey =
        (userKeyStr == null) ? getCurrentUserKey() : OfyUtil.<User>createKey(userKeyStr);
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
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
    Key<User> userKey =
        (userKeyStr == null) ? getCurrentUserKey() : OfyUtil.<User>createKey(userKeyStr);
    User.updateMembership(userKey, orgKey, null);
  }

  @Path("{org}/leaderboard")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Leaderboard getLeaderboard(
      @PathParam("org") String orgKeyStr,
      @QueryParam(LEADERBOARD_TYPE_PARAM) LeaderboardType type) {
    if (type == null) {
      type = LeaderboardType.ALL_TIME;
    }
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
    return ofy().load().key(Leaderboard.createKey(orgKey, type)).now();
  }

  @Path("{org_key}/waiver")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<WaiverSummaryView> getWaivers(
      @PathParam("org_key") String orgKeyStr,
      @DefaultValue("false") @QueryParam(INCLUDE_PARENT_ORGS_PARAM) boolean includeParentOrgs) {
    Key<Organization> rootOrgKey =
        OfyUtil.<Organization>createKey(orgKeyStr);
    List<Organization> orgs;
    if (includeParentOrgs) {
      orgs = Organization.getOrgAndAncestorOrgs(rootOrgKey);
    } else {
      orgs = Lists.newArrayList(ofy().load().key(rootOrgKey).now());
    }

    // Asynchronously launch the queries to fetch the waivers.
    List<Pair<Organization, ? extends Iterable<Waiver>>> orgsAndWaivers = Lists.newArrayList();
    for (Organization org : orgs) {
      orgsAndWaivers.add(ImmutablePair.of(org,
        ofy().load().type(Waiver.class).ancestor(Key.create(org)).iterable()));
    }

    // Create the waiver summaries.
    List<WaiverSummaryView> waiverSummaries = Lists.newArrayList();
    for (Pair<Organization, ? extends Iterable<Waiver>> orgAndWaiver : orgsAndWaivers) {
      waiverSummaries.addAll(
        WaiverSummaryView.create(orgAndWaiver.getLeft(), orgAndWaiver.getRight()));
    }
    Collections.sort(waiverSummaries, WaiverSummaryView.OrgAndDescriptionComparator.INSTANCE);
    return ListResponseMsg.create(waiverSummaries);
  }

  @Path("{org_key}/waiver")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertWaiver(
      @PathParam("org_key") String orgKeyStr,
      Waiver waiver) {
    Key<Organization> orgKey = OfyUtil.<Organization>createKey(orgKeyStr);
    if (waiver == null) {
      throw ErrorResponseMsg.createException("waiver argument not specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    Waiver.insert(orgKey, waiver);
    URI uri = uriInfo.getBaseUriBuilder().path(WaiverResource.PATH).path(waiver.getKey()).build();
    return Response.created(uri).build();
  }
}
