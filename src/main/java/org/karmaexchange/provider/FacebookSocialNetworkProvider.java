package org.karmaexchange.provider;

import static java.lang.String.format;
import static org.karmaexchange.security.OAuthFilter.OAUTH_LOG_LEVEL;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.servlet.Filter;

import lombok.Data;
import lombok.Getter;

import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.AgeRange;
import org.karmaexchange.dao.Gender;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.appengine.api.datastore.GeoPt;
import com.restfb.DefaultFacebookClient;
import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.Location;
import com.restfb.types.Page;

public final class FacebookSocialNetworkProvider extends SocialNetworkProvider {

  private static final Logger logger = Logger.getLogger(Filter.class.getName());

  private static final String PROFILE_IMAGE_URL_FMT = "https://graph.facebook.com/%s/picture";

  public FacebookSocialNetworkProvider(OAuthCredential credential,
      SocialNetworkProviderType providerType) {
    super(credential, providerType);
  }

  @Override
  public boolean verifyCredential() {
    return verifyCredential(credential);
  }

  public static boolean verifyCredential(OAuthCredential credential) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    com.restfb.types.User fbUser =
        fetchObject(fbClient, "me", com.restfb.types.User.class, Parameter.with("fields", "id"));
    if (fbUser.getId().equals(credential.getUid())) {
      return true;
    } else {
      logger.log(OAUTH_LOG_LEVEL,
        format("Uid of user=[%s] does not match credential uid=[%s]",
          fbUser.getId(), credential.getUid()));
      return false;
    }
  }

  @Override
  public User createUser() {
    return createUser(credential);
  }

  public static User createUser(OAuthCredential credential) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    // Getting age_range unfortunately requires explicitly specifiying the fields.
    ExtFbUser fbUser = fetchObject(fbClient, "me", ExtFbUser.class,
      Parameter.with("fields", "id, first_name, last_name, email, location, age_range, gender"));
    User user = User.create(credential);
    user.setFirstName(fbUser.getFirstName());
    user.setLastName(fbUser.getLastName());
    if (fbUser.getEmail() != null) {
      user.getRegisteredEmails().add(new RegisteredEmail(fbUser.getEmail(), true));
    }
    user.setAddress(parseCity(fbUser));
    user.setGender(parseGender(fbUser));
    user.setAgeRange(parseAgeRange(fbUser));
    return user;
  }

  private static Address parseCity(com.restfb.types.User fbUser) {
    if ((fbUser.getLocation() != null) && (fbUser.getLocation().getName() != null)) {
      return parseCity(fbUser.getLocation().getName());
    } else {
      return null;
    }
  }

  public static Address parseCity(String currentCity) {
    Address address = new Address();
    String[] cityState = currentCity.split(",");
    address.setCity(cityState[0].trim());
    if (cityState.length > 1) {
      address.setState(cityState[1].trim());
    }
    // TODO(avaliani): use the fb location id to get a complete address.
    return address;
  }

  private static Gender parseGender(com.restfb.types.User fbUser) {
    return (fbUser.getGender() == null) ? null : Gender.valueOf(fbUser.getGender().toUpperCase());
  }

  private static AgeRange parseAgeRange(ExtFbUser fbUser) {
    ExtFbUser.FbAgeRange fbAgeRange = fbUser.getAgeRange();
    if (fbAgeRange == null) {
      return null;
    }
    if (fbAgeRange.min == null) {
      logger.log(Level.WARNING, "Failed to parse facebook age_range: no min" + fbAgeRange);
      return null;
    }
    return new AgeRange(fbAgeRange.min, fbAgeRange.max);
  }

  @Override
  public String getProfileImageUrl() {
    return String.format(PROFILE_IMAGE_URL_FMT, credential.getUid());
  }

  @Override
  public Organization createOrganization(String pageUrl) throws URISyntaxException {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(credential.getToken());
    // Getting age_range unfortunately requires explicitly specifiying the fields.
    String fbPageName = getPageNameFromUrl(pageUrl);
    Page fbPage = fetchObject(fbClient, fbPageName, Page.class);

    Organization org = new Organization();
    org.setName(fbPageName);
    org.setOrgName(fbPage.getName());
    org.setPage(PageRef.create(pageUrl, providerType));
    org.setAddress(getAddress(fbPage.getLocation()));
    return org;
  }

  private Address getAddress(@Nullable Location fbLocation) {
    if (fbLocation == null) {
      return null;
    }
    Address address = new Address();
    address.setStreet(fbLocation.getStreet());
    address.setCity(fbLocation.getCity());
    address.setState(fbLocation.getState());
    address.setCountry(fbLocation.getCountry());
    address.setZip(fbLocation.getZip());
    if ((fbLocation.getLatitude() != null) && (fbLocation.getLongitude() != null)) {
      address.setGeoPt(GeoPtWrapper.create(
        new GeoPt(fbLocation.getLatitude().floatValue(), fbLocation.getLongitude().floatValue())));
    }
    return address;
  }

  public static <T> T fetchObject(FacebookClient fbClient, String name, Class<T> objClass,
      Parameter... parameters) {
    try {
      return fbClient.fetchObject(name, objClass, parameters);
    } catch(FacebookException e) {
      throw ErrorResponseMsg.createException(e,
        (e instanceof FacebookOAuthException) ? ErrorInfo.Type.AUTHENTICATION :
          ErrorInfo.Type.PARTNER_SERVICE_FAILURE);
    }
  }

  public static class ExtFbUser extends com.restfb.types.User {

    private static final long serialVersionUID = 1L;

    @Facebook("age_range")
    @Getter
    private FbAgeRange ageRange;

    @Data
    public static class FbAgeRange {
      @Facebook
      private Integer min;

      @Facebook
      private Integer max;
    }
  }
}
