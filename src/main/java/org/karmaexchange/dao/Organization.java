package org.karmaexchange.dao;

import static com.google.common.base.CharMatcher.WHITESPACE;
import static org.karmaexchange.util.UserService.getCurrentUserCredential;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.net.URISyntaxException;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.provider.SocialNetworkProvider;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ValidationErrorInfo;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationError;
import org.karmaexchange.resources.msg.ValidationErrorInfo.ValidationErrorType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Organization extends NameBaseDao<Organization> {

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

  private String orgName;
  @Index
  private String searchableOrgName;

  private PageRef page;
  private KeyWrapper<Organization> parentPage;

  // TODO(avaliani): Should we replicate this info or always fetch it from facebook from the UI?
  // private String about;
  // private String website;
  // private ContactInfo contactInfo;

  // Email address is not a field in facebook pages.
  private String email;

  private Address address;

  private List<KeyWrapper<CauseType>> causes = Lists.newArrayList();

  private List<KeyWrapper<User>> admins = Lists.newArrayList();

  // Users that have given permission for Organizations to use their ids as organizers for events.
  // By default admins are organizers.
  private List<KeyWrapper<User>> organizers = Lists.newArrayList();

  @Index
  private long karmaPoints;
  private IndexedAggregateRating eventRating;

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
    // Add the current user as an admin if no admins are specified
    if (admins.isEmpty()) {
      admins.add(KeyWrapper.create(getCurrentUserKey()));
    }

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

    if (admins.isEmpty()) {
      validationErrors.add(new ResourceValidationError(
        this, ValidationErrorType.RESOURCE_FIELD_VALUE_REQUIRED, "admins"));
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
    for (KeyWrapper<User> adminKeyWrapper : admins) {
      if (KeyWrapper.toKey(adminKeyWrapper).equals(getCurrentUserKey())) {
        return Permission.ALL;
      }
    }
    return Permission.READ;
  }
}
