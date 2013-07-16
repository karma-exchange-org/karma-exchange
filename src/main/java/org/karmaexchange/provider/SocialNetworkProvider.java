package org.karmaexchange.provider;

import java.net.URI;
import java.net.URISyntaxException;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.User;

public abstract class SocialNetworkProvider {
  protected final OAuthCredential credential;
  protected final SocialNetworkProviderType providerType;

  /* If a new SocialNetworkProviderType is added, make sure to modify ImageProviderType */
  public enum SocialNetworkProviderType {
    FACEBOOK {
      @Override
      public SocialNetworkProvider getProvider(OAuthCredential credential) {
        return new FacebookSocialNetworkProvider(credential, this);
      }
    };

    public abstract SocialNetworkProvider getProvider(OAuthCredential credential);

    public String getOAuthProviderName() {
      return name();
    }
  }

  protected SocialNetworkProvider(OAuthCredential credential,
      SocialNetworkProviderType providerType) {
    this.credential = credential;
    this.providerType = providerType;
  }

  public SocialNetworkProviderType getProviderType() {
    return providerType;
  }

  public abstract boolean verifyCredential();

  public abstract User createUser();

  public abstract String getProfileImageUrl();

  public abstract Organization createOrganization(String pageUrl) throws URISyntaxException;

  public static String getPageNameFromUrl(String pageUrl) throws URISyntaxException {
    String pagePath = new URI(pageUrl).getPath();
    if (!pagePath.startsWith("/")) {
      throw new URISyntaxException(pagePath, "page path not specified");
    }
    String objectName = pagePath.substring(1);
    if (objectName.contains("/")) {
      throw new URISyntaxException(objectName, "base url for page required");
    }
    return objectName;
  }
}
