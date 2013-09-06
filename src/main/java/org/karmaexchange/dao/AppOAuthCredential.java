package org.karmaexchange.dao;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class AppOAuthCredential extends OAuthCredential {

  public AppOAuthCredential(SocialNetworkProviderType type, String token) {
    super(type.getOAuthProviderName(), null, token);
  }

  @Override
  public void setUid(String uid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getUid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getGlobalUid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getGlobalUidAndToken() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void updateGlobalUidFields() {
    // Nothing to do.
  }

  @Override
  public String toString() {
    return "{app}" + "@" + provider + ":" + token;
  }
}
