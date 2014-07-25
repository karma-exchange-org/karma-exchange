package org.karmaexchange.dao;

import static com.google.common.base.CharMatcher.WHITESPACE;
import static java.util.Arrays.asList;
import static org.karmaexchange.util.OfyService.ofy;
import static org.karmaexchange.util.UserService.getCurrentUserKey;
import static org.karmaexchange.util.UserService.isCurrentUserAdmin;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.validator.routines.EmailValidator;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.BaseDaoView;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;
import org.karmaexchange.task.UpdateNamedKeysAdminTaskServlet;
import org.karmaexchange.util.SearchUtil.ReservedToken;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Organization extends NameBaseDao<Organization> implements BaseDaoView<Organization> {

  private String orgName;
  @Index
  private String searchableOrgName;

  private PageRef page;

  // This field non-null only if the organization itself does not have a facebook page.
  //
  // We need the listing org page info so we display an icon / cover photo for
  // automatically created organizations.
  @Nullable
  private PageRef listingOrgPage;

  private String mission;
  @Index
  private OrganizationNamedKeyWrapper parentOrg;

  // TODO(avaliani): Should we replicate this info or always fetch it from facebook from the UI?
  // private String about;
  // private String website;
  // private ContactInfo contactInfo;

  // Email address is not a field in facebook pages.
  private String email;

  private List<AutoMembershipRule> autoMembershipRules = Lists.newArrayList();

  private Address address;

  private List<CauseType> causes = Lists.newArrayList();

  @Index
  private long karmaPoints;
  private IndexedAggregateRating eventRating;

  @Ignore
  private String searchTokenSuffix;

  private String donationUrl;

  @Nullable
  private SourceOrganizationInfo sourceOrgInfo;

  public enum Role {
    ADMIN(3),
    ORGANIZER(2),
    MEMBER(1);

    private int capabilityLevel;

    private Role(int capabilityLevel) {
      this.capabilityLevel = capabilityLevel;
    }

    public boolean hasEqualOrMoreCapabilities(Role otherRole) {
      return capabilityLevel >= otherRole.capabilityLevel;
    }
  }

  /*
   * TODO(avaliani): is the type information useful for people to know? Non-profits and for
   * profits can both generate volunteer events.
   *
   * | public enum Type {
   * |   NON_PROFIT,
   * |   COMMERCIAL
   * | }
   * | private Type type;
  */

  public static String orgIdToName(String name) {
    // We're making the name case insensitive
    return name.toLowerCase();
  }

  public static String getOrgId(Key<Organization> orgKey) {
    return orgKey.getName();
  }

  public static Key<Organization> createKey(String orgId) {
    return Key.create(Organization.class, orgIdToName(orgId));
  }

  public static String getPrimaryOrgSearchToken(Key<Organization> orgKey) {
    return ReservedToken.PRIMARY_ORG.create(orgKey.getName());
  }

  public static String getAssociatedOrgsSearchToken(Key<Organization> orgKey) {
    return ReservedToken.ORG.create(orgKey.getName());
  }

  public String getSearchTokenSuffix() {
    // TODO(avaliani): Potential bug. If two orgs have different page names unparsed but the
    // same page name parsed we have a collision. We should eventually fix parseTokenSuffix to
    // handle this.
    return ReservedToken.parseTokenSuffix(name);
  }

  public void initFromPage() {
    owner = null;
    if ((page == null) || !page.isValid()) {
      throw ValidationErrorInfo.createException(ImmutableList.of(
        new ResourceValidationError(this,
          ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "page")));
    }

    // Right now we only support facebook.
    SocialNetworkProvider provider =
        SocialNetworkProviderType.FACEBOOK.getProvider();
    provider.initOrganization(this, page.getName());
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    if (orgName != null) {
      orgName = WHITESPACE.trimFrom(orgName);
      searchableOrgName = orgName.toLowerCase();
    }
    updateParentOrgName();
    // For now, avoid doing this unless we are sure the email address is not a generic email
    // address like gmail, yahoo, hotmail, etc. We don't want to accidentally give permissions
    // to arbitrary users. The UI can do this and warn the user.
    //
    //    if (domains.isEmpty() && (email != null)) {
    //      String[] splitEmail = email.split("@", 2);
    //      if (splitEmail.length > 1) {
    //        domains.add(splitEmail[1]);
    //      }
    //    }

    eventRating = IndexedAggregateRating.create();
    karmaPoints = 0;

    validateOrganization();
  }

  @Override
  protected void processUpdate(Organization oldOrg) {
    super.processUpdate(oldOrg);
    if (orgName != null) {
      orgName = WHITESPACE.trimFrom(orgName);
      searchableOrgName = orgName.toLowerCase();
    }
    if (!Objects.equal(parentOrg, oldOrg.parentOrg)) {
      updateParentOrgName();
    }

    // Restore fields that are explicitly updated by the backend.
    karmaPoints = oldOrg.karmaPoints;
    eventRating = oldOrg.eventRating;

    validateOrganizationUpdate(oldOrg);
    validateOrganization();

    if (!orgName.equals(oldOrg.orgName)) {
      UpdateNamedKeysAdminTaskServlet.enqueueTask(Key.create(this));
    }
  }

  private void updateParentOrgName() {
    if (parentOrg != null) {
      try {
        parentOrg.updateName();
      } catch (IllegalArgumentException e) {
        throw ValidationErrorInfo.createException(asList(
          new ResourceValidationError(this,
            ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "parentOrg")));
      }
    }
  }

  private void validateOrganization() {
    List<ValidationError> validationErrors = Lists.newArrayList();

    if ((orgName == null) || orgName.isEmpty()) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "orgName"));
    }

    if (page == null) {
      if (listingOrgPage == null) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "page | listingOrgPage"));
      } else if (!listingOrgPage.isValid()) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "listingOrgPage"));
      }
    } else {
      if (!page.isValid()) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "page"));
      } else if (!orgIdToName(page.getName()).equals(name)) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "name"));
      }
    }

    if ((email != null) && !EmailValidator.getInstance().isValid(email)) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "email"));
    }
    for (AutoMembershipRule autoMembershipRule : autoMembershipRules) {
      validationErrors.addAll(autoMembershipRule.validate(this));
    }

    if (!validationErrors.isEmpty()) {
      throw ValidationErrorInfo.createException(validationErrors);
    }
  }

  private void validateOrganizationUpdate(Organization oldOrg) {
    List<ValidationError> validationErrors = Lists.newArrayList();

    if (!oldOrg.page.equals(page)) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_UNMODIFIABLE, "page"));
    }

    if (!validationErrors.isEmpty()) {
      throw ValidationErrorInfo.createException(validationErrors);
    }
  }

  // TODO(avaliani): remove the load in eval permissions.
  @Override
  protected Permission evalPermission() {
    if (isCurrentUserOrgAdmin()) {
      return Permission.ALL;
    }
    return Permission.READ;
  }

  /*
   * Note that this call fetches the current user's user object to evaluate the users role in
   * the org.
   */
  @XmlTransient
  public boolean isCurrentUserOrgAdmin() {
    if (isCurrentUserAdmin()) {
      return true;
    }
    User currentUser = ofy().transactionless().load().key(getCurrentUserKey()).now();
    if (currentUser == null) {
      return false;
    }
    // In the future we can support hierarchical ADMIN roles if people request it.
    return currentUser.hasOrgMembership(Key.create(this), Role.ADMIN);
  }

  public static class OrgNameComparator implements Comparator<Organization> {
    public static final OrgNameComparator INSTANCE = new OrgNameComparator();

    @Override
    public int compare(Organization org1, Organization org2) {
      return org1.searchableOrgName.compareTo(org2.searchableOrgName);
    }
  }

  /**
   * This class represents the roles that can be granted automatically based upon user email
   * domain based membership.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AutoMembershipRule {
    private String domain;
    private Role maxGrantableRole;

    public List<ValidationError> validate(Organization org) {
      List<ValidationError> validationErrors = Lists.newArrayList();
      if ((!EmailValidator.getInstance().isValid("user@" + domain)) ||
          (maxGrantableRole == null)) {
        validationErrors.add(new ResourceValidationError(
          org, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID,
          "autoMembershipRules[domain=\"" + domain + "\"]"));
      }
      return validationErrors;
    }

    public static Predicate<AutoMembershipRule> emailPredicate(final String email) {
      return new Predicate<AutoMembershipRule>() {
        @Override
        public boolean apply(@Nullable AutoMembershipRule input) {
          return email.toLowerCase().endsWith("." + input.domain.toLowerCase()) ||
              email.toLowerCase().endsWith("@" + input.domain.toLowerCase());
        }
      };
    }
  }

  public boolean canAutoGrantMembership(String email, Role reqRole) {
    AutoMembershipRule rule = Iterables.tryFind(autoMembershipRules,
      AutoMembershipRule.emailPredicate(email)).orNull();
    return (rule != null) &&
        rule.getMaxGrantableRole().hasEqualOrMoreCapabilities(reqRole);
  }

  public static List<Key<Organization>> getOrgAndAncestorOrgKeys(Key<Organization> orgKey) {
    List<Key<Organization>> allOrgs = Lists.newArrayList();
    for (Organization org : getOrgAndAncestorOrgs(orgKey)) {
      allOrgs.add(Key.create(org));
    }
    return allOrgs;
  }

  public static List<Organization> getOrgAndAncestorOrgs(Key<Organization> orgKey) {
    List<Organization> allOrgs = Lists.newArrayList();
    while (orgKey != null) {
      Organization org = ofy().transactionless().load().key(orgKey).now();
      orgKey = null;  // For next iteration.
      if (org != null) {
        allOrgs.add(org);
        if (org.parentOrg != null) {
          orgKey = KeyWrapper.toKey(org.parentOrg);
        }
      }
    }
    return allOrgs;
  }

  @Override
  public void updateDependentNamedKeys() {
    Iterable<Key<Organization>> childOrgKeys =
        ofy().load().type(Organization.class).filter("parentOrg.key", Key.create(this)).keys();
    for (Key<Organization> childOrgKey : childOrgKeys) {
      ofy().transact(new UpdateDependentNamedKeyTxn(childOrgKey,
        new OrganizationNamedKeyWrapper(this)));
    }
  }

  @Data
  @EqualsAndHashCode(callSuper=false)
  public static class UpdateDependentNamedKeyTxn extends VoidWork {
    private final Key<Organization> childOrgKey;
    private final OrganizationNamedKeyWrapper parentOrgNamedKey;

    public void vrun() {
      Organization childOrg = ofy().load().key(childOrgKey).now();
      if ((childOrg != null) && (childOrg.parentOrg != null) &&
          childOrg.parentOrg.getKey().equals(parentOrgNamedKey.getKey()) &&
          !childOrg.parentOrg.getName().equals(parentOrgNamedKey.getName())) {
        childOrg.parentOrg = parentOrgNamedKey;
        BaseDao.partialUpdate(childOrg);
      }
    }
  }

  @Override
  public Organization getDao() {
    return this;
  }

  @Data
  @NoArgsConstructor
  public static final class SourceOrganizationInfo {
    // An external id uniquely identifying the source org.
    private String id;

    // Null if the organization is the same as the listing organization.
    @Nullable
    KeyWrapper<Organization> listingOrg;

    public SourceOrganizationInfo(String id, Key<Organization> listingOrgKey) {
      this.id = id;
      this.listingOrg = KeyWrapper.create(listingOrgKey);
    }
  }
}
