package org.karmaexchange.resources.msg;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.OrganizationMembership;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

@XmlRootElement
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class OrganizationMemberView extends UserSummaryInfoView {

  private Organization.Role role;

  public static List<OrganizationMemberView> create(Collection<User> usersBatch,
      Key<Organization> orgKey) {
    List<OrganizationMemberView> members = Lists.newArrayList();
    for (User user : usersBatch) {
      OrganizationMembership membership = user.tryFindOrganizationMembership(orgKey);
      if (membership != null) {
        // Query may have found stale index entries.
        members.add(new OrganizationMemberView(user, membership));
      }
    }
    return members;
  }

  public OrganizationMemberView(User user, OrganizationMembership membership) {
    super(user);
    role = membership.getRole();
  }
}
