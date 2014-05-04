package org.karmaexchange.provider;

import org.karmaexchange.auth.AuthProvider;
import org.karmaexchange.dao.Organization;

public interface SocialNetworkProvider extends AuthProvider {

  /* If a new SocialNetworkProviderType is added, make sure to modify ImageProviderType */
  public enum SocialNetworkProviderType {
    FACEBOOK {
      @Override
      public SocialNetworkProvider getProvider() {
        return new FacebookSocialNetworkProvider();
      }
    };

    public abstract SocialNetworkProvider getProvider();
  }

  Organization createOrganization(String pageName);
}
