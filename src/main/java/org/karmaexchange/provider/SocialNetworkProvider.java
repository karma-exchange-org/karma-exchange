package org.karmaexchange.provider;

import org.karmaexchange.dao.OAuthCredential;

public abstract class SocialNetworkProvider {

  public abstract boolean verifyCredential(OAuthCredential credential);
}
