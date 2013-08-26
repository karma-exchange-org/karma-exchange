package org.karmaexchange.resources.msg;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.AggregateRating;
import org.karmaexchange.dao.CauseType;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.OrganizationMembership;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public class OrganizationMembershipView {

  private KeyWrapper<Organization> org;
  private String orgName;
  private PageRef orgPage;
  private long orgKarmaPoints;
  private AggregateRating orgEventRating;
  private List<KeyWrapper<CauseType>> orgCauses;
  private String searchTokenSuffix;

  @Nullable
  private Organization.Role role;
  @Nullable
  private Organization.Role requestedRole;

  public static List<OrganizationMembershipView> create(User user) {
    // The number of memberships a user should have is small. Therefore, fetch them all and
    // then sort them.
    Map<Key<Organization>, OrganizationMembership> membershipMap = Maps.newHashMap();
    for (OrganizationMembership membership : user.getOrganizationMemberships()) {
      membershipMap.put(KeyWrapper.toKey(membership.getOrganization()), membership);
    }
    List<Organization> organizations = Lists.newArrayList();
    for (Organization org : ofy().load().keys(membershipMap.keySet()).values()) {
      if (org != null) {
        organizations.add(org);
      }
    }
    Collections.sort(organizations, Organization.OrgNameComparator.INSTANCE);
    List<OrganizationMembershipView> membershipViewList = Lists.newArrayList();
    for (Organization org : organizations) {
      membershipViewList.add(
        new OrganizationMembershipView(org, membershipMap.get(Key.create(org))));
    }
    return membershipViewList;
  }

  private OrganizationMembershipView(Organization org, OrganizationMembership membership) {
    this.org = KeyWrapper.create(org);
    orgName = org.getOrgName();
    orgPage = org.getPage();
    orgKarmaPoints = org.getKarmaPoints();
    orgEventRating = org.getEventRating();
    orgCauses = org.getCauses();
    role = membership.getRole();
    requestedRole = membership.getRequestedRole();
    searchTokenSuffix = org.getSearchTokenSuffix();
  }
}
