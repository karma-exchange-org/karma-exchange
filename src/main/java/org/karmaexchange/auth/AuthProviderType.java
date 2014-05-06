package org.karmaexchange.auth;

import org.karmaexchange.provider.FacebookSocialNetworkProvider;
import org.karmaexchange.provider.MozillaPersonaAuthProvider;

public enum AuthProviderType {
  FACEBOOK {
    @Override
    public AuthProvider getProvider() {
      return new FacebookSocialNetworkProvider();
    }
  },
  MOZILLA_PERSONA {
    @Override
    public AuthProvider getProvider() {
      return new MozillaPersonaAuthProvider();
    }
  };

  public abstract AuthProvider getProvider();
}
