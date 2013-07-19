package org.karmaexchange.dao;

import static com.google.common.base.CharMatcher.WHITESPACE;
import static org.karmaexchange.util.UserService.getCurrentUser;
import static org.karmaexchange.util.UserService.getCurrentUserCredential;
import static org.karmaexchange.util.UserService.getCurrentUserKey;
import static org.karmaexchange.util.UserService.isCurrentUserAdmin;

import java.net.URISyntaxException;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.validator.routines.EmailValidator;
import org.karmaexchange.dao.User.OrganizationMembership;
import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;
import org.karmaexchange.task.AddOrgAdminServlet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Organization extends NameBaseDao<Organization> {

  private String orgName;
  @Index
  private String searchableOrgName;

  private PageRef page;
  private KeyWrapper<Organization> parentOrg;

  // TODO(avaliani): Should we replicate this info or always fetch it from facebook from the UI?
  // private String about;
  // private String website;
  // private ContactInfo contactInfo;

  // Email address is not a field in facebook pages.
  private String email;

  private List<String> domains = Lists.newArrayList();

  private Address address;

  private List<KeyWrapper<CauseType>> causes = Lists.newArrayList();

  @Index
  private long karmaPoints;
  private IndexedAggregateRating eventRating;

  public enum Role {
    ADMIN(3),
    ORGANIZER(2),
    MEMBER(1);

    private int capabilityLevel;

    private Role(int capabilityLevel) {
      this.capabilityLevel = capabilityLevel;
    }

    public boolean hasEqualOrMoreCapabilities(Role prevRole) {
      return capabilityLevel >= prevRole.capabilityLevel;
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

  @Override
  public void setName(String name) {
    // Facebook page names are case insensitive.
    this.name = getNameFromPageName(name);
  }

  public static String getNameFromPageName(String pageName) {
    return pageName.toLowerCase();
  }

  public void initFromPage() {
    owner = null;
    if (!pageIsInitialized()) {
      throw ValidationErrorInfo.createException(ImmutableList.of(
        new ResourceValidationError(this,
          ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "page")));
    }
    SocialNetworkProvider provider =
        SocialNetworkProviderFactory.getProvider(getCurrentUserCredential());
    if (provider.getProviderType() != page.getUrlProvider()) {
      throw ValidationErrorInfo.createException(ImmutableList.of(
        new ResourceValidationError(this,
          ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "page.urlProvider")));
    }
    Organization providerGeneratedOrg;
    try {
      providerGeneratedOrg = provider.createOrganization(page.getUrl());
    } catch (URISyntaxException e) {
      throw ValidationErrorInfo.createException(ImmutableList.of(
        new ResourceValidationError(this,
          ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "page.url")));
    }
    name = getNameFromPageName(providerGeneratedOrg.getName());
    if (orgName == null) {
      orgName = providerGeneratedOrg.getOrgName();
    }
    if (address == null) {
      address = providerGeneratedOrg.getAddress();
    }
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    if (orgName != null) {
      orgName = WHITESPACE.trimFrom(orgName);
      searchableOrgName = orgName.toLowerCase();
    }
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

    // Transactionally add the current user as the admin. This will be replaced later by
    // an email verification process.
    AddOrgAdminServlet.enqueueTask(Key.create(this), getCurrentUserKey());
  }

  @Override
  protected void processUpdate(Organization oldOrg) {
    super.processUpdate(oldOrg);
    if (orgName != null) {
      orgName = WHITESPACE.trimFrom(orgName);
      searchableOrgName = orgName.toLowerCase();
    }

    // Restore fields that are explicitly updated by the backend.
    karmaPoints = oldOrg.karmaPoints;
    eventRating = oldOrg.eventRating;

    validateOrganizationUpdate(oldOrg);
    validateOrganization();
  }

  private void validateOrganization() {
    List<ValidationError> validationErrors = Lists.newArrayList();

    if ((orgName == null) || orgName.isEmpty()) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "orgName"));
    }

    if (pageIsInitialized()) {
      String pageDerivedName = null;
      try {
        pageDerivedName = SocialNetworkProvider.getPageNameFromUrl(page.getUrl());
      } catch (URISyntaxException e) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "page.url"));
      }
      if ((pageDerivedName != null) && !getNameFromPageName(pageDerivedName).equals(name)) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "name"));
      }
    } else {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "page"));
    }

    if ((email != null) && !EmailValidator.getInstance().isValid(email)) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "email"));
    }
    for (String domain : domains) {
      if (!EmailValidator.getInstance().isValid("user@" + domain)) {
        validationErrors.add(new ResourceValidationError(
          this, ValidationErrorType.RESOURCE_FIELD_VALUE_INVALID, "domains"));
      }
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

  private boolean pageIsInitialized() {
    return (page != null) || (page.getUrlProvider() != null) || (page.getUrl() != null);
  }

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
  public boolean isCurrentUserOrgAdmin() {
    if (isCurrentUserAdmin()) {
      return true;
    }
    User currentUser = getCurrentUser();
    if (currentUser == null) {
      throw ErrorResponseMsg.createException("current user not found", ErrorInfo.Type.BAD_REQUEST);
    }
    // In the future we can support hierarchical ADMIN roles if people request it.
    OrganizationMembership membership = currentUser.tryFindOrganizationMembership(Key.create(this));
    return (membership != null) && (membership.getRole() == Role.ADMIN);
  }
}
