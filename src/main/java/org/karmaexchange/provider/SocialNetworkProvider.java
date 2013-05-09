package org.karmaexchange.provider;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;

public abstract class SocialNetworkProvider {
  protected OAuthCredential credential;

  protected SocialNetworkProvider(OAuthCredential credential) {
    this.credential = credential;
  }

  public abstract boolean verifyCredential();

  public abstract User initUser();
}
