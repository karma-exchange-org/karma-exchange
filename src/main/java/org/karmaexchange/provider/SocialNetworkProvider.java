package org.karmaexchange.provider;

import org.karmaexchange.dao.OAuthCredential;

public abstract class SocialNetworkProvider {
  protected OAuthCredential credential;
  private SocialNetworkProviderType providerType;

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

  public abstract String getProfileImageUrl();
}
