package org.karmaexchange.provider;

import static org.karmaexchange.util.Properties.Property.FACEBOOK_APP_ID;
import static org.karmaexchange.util.Properties.Property.FACEBOOK_APP_SECRET;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import lombok.Data;
import lombok.Getter;

import org.karmaexchange.auth.AuthProviderCredentials;
import org.karmaexchange.auth.AuthProviderType;
import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidType;
import org.karmaexchange.dao.Address;
import org.karmaexchange.dao.AgeRange;
import org.karmaexchange.dao.Gender;
import org.karmaexchange.dao.GeoPtWrapper;
import org.karmaexchange.dao.ImageProviderType;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.PageRef;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.Properties;

import com.google.appengine.api.datastore.GeoPt;
import com.restfb.DefaultFacebookClient;
import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.Location;
import com.restfb.types.Page;

public final class FacebookSocialNetworkProvider implements SocialNetworkProvider {

  private static final Logger logger =
      Logger.getLogger(FacebookSocialNetworkProvider.class.getName());

  private static final String PROFILE_IMAGE_URL_FMT = "https://graph.facebook.com/%s/picture";
  public static final String PAGE_BASE_URL = "https://www.facebook.com/";

  @Override
  public CredentialVerificationResult verifyUserCredentials(AuthProviderCredentials userCredentials,
      HttpServletRequest req) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(userCredentials.getToken());
    com.restfb.types.User fbUser =
        fetchObject(fbClient, "me", com.restfb.types.User.class, Parameter.with("fields", "id"));
    return new CredentialVerificationResult(
      new GlobalUid(GlobalUidType.toGlobalUidType(AuthProviderType.FACEBOOK), fbUser.getId()),
      new FacebookCredentialVerificationCtx(userCredentials.getToken()));
  }

  @Override
  public UserInfo createUser(CredentialVerificationResult verificationResult) {
    FacebookCredentialVerificationCtx ctx =
        (FacebookCredentialVerificationCtx) verificationResult.getVerificationCtx();
    DefaultFacebookClient fbClient = new DefaultFacebookClient(ctx.getAuthToken());
    // Getting age_range unfortunately requires explicitly specifiying the fields.
    ExtFbUser fbUser = fetchObject(fbClient, "me", ExtFbUser.class,
      Parameter.with("fields", "id, first_name, last_name, email, location, age_range, gender"));
    User user = User.create();
    user.setFirstName(fbUser.getFirstName());
    user.setLastName(fbUser.getLastName());
    if (fbUser.getEmail() != null) {
      user.getRegisteredEmails().add(new RegisteredEmail(fbUser.getEmail(), true));
    }
    user.setAddress(parseCity(fbUser));
    user.setGender(parseGender(fbUser));
    user.setAgeRange(parseAgeRange(fbUser));
    return new UserInfo(user,
      new UserInfo.ProfileImage(
        ImageProviderType.FACEBOOK,
        getProfileImageUrl(fbUser.getId())) );
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

  public static String getProfileImageUrl(String fbUserId) {
    return String.format(PROFILE_IMAGE_URL_FMT, fbUserId);
  }

  @Override
  public void initOrganization(Organization org, String fbPageName) {
    DefaultFacebookClient fbClient = new DefaultFacebookClient(getAppCredentials().getToken());
    // Getting age_range unfortunately requires explicitly specifiying the fields.
    Page fbPage = fetchObject(fbClient, fbPageName, Page.class);

    // Relying on fb page name uniqueness
    org.setName(Organization.orgIdToName(fbPageName));
    org.setOrgName(fbPage.getName());
    org.setPage(PageRef.create(fbPageName, PAGE_BASE_URL + fbPageName,
      SocialNetworkProviderType.FACEBOOK));
    String mission = null;
    if (fbPage.getMission() != null) {
      mission = fbPage.getMission();
    } else if (fbPage.getAbout() != null) {
      mission = fbPage.getAbout();
    }
    org.setMission(mission);
    org.setAddress(getAddress(fbPage.getLocation()));
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
      if (e instanceof FacebookOAuthException) {
        throw ErrorResponseMsg.createException(e.getMessage(), ErrorInfo.Type.AUTHENTICATION);
      } else {
        throw ErrorResponseMsg.createException(e, ErrorInfo.Type.PARTNER_SERVICE_FAILURE);
      }
    }
  }

  private static AuthProviderCredentials getAppCredentials() {
    String appId = Properties.get(FACEBOOK_APP_ID);
    String appSecret = Properties.get(FACEBOOK_APP_SECRET);
    AccessToken accessToken =
        new DefaultFacebookClient().obtainAppAccessToken(appId, appSecret);
    return new AuthProviderCredentials(accessToken.getAccessToken());
  }

  @Data
  private static class FacebookCredentialVerificationCtx implements CredentialVerificationCtx {
    private final String authToken;
  }

  private static class ExtFbUser extends com.restfb.types.User {

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
