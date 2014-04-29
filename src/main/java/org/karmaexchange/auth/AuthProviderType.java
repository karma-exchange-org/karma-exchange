package org.karmaexchange.auth;

import org.karmaexchange.provider.FacebookSocialNetworkProvider;

public enum AuthProviderType {
  FACEBOOK {
    @Override
    public AuthProvider getProvider() {
      return new FacebookSocialNetworkProvider();
    }
  },
  MOZILLA {
    @Override
    public AuthProvider getProvider() {
      // TODO(avaliani): fill this in
      return null;
    }
  };

  public abstract AuthProvider getProvider();
}
