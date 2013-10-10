package org.karmaexchange.provider;

import java.net.URI;

import javax.servlet.ServletContext;

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

      @Override
      public OAuthCredential getAppCredential(ServletContext servletCtx, URI requestUri) {
        return FacebookSocialNetworkProvider.getAppCredential(servletCtx, requestUri);
      }
    };

    public abstract SocialNetworkProvider getProvider(OAuthCredential credential);

    public abstract OAuthCredential getAppCredential(ServletContext servletCtx, URI requestUri);

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

  public abstract Organization createOrganization(String pageName);
}
