package org.karmaexchange.dao;

import javax.annotation.Nullable;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.google.common.base.Predicate;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Index;

@Embed
@EqualsAndHashCode
@NoArgsConstructor
public class OAuthCredential {
  @Getter
  protected String provider;
  @Nullable
  @Getter
  protected String uid;
  @Getter
  protected String token;

  @Getter
  protected String globalUid;
  @Index
  @Getter
  protected String globalUidAndToken;

  public static OAuthCredential create(String provider, String uid, String token) {
    return new OAuthCredential(provider, uid, token);
  }

  protected OAuthCredential(String provider, @Nullable String uid, String token) {
    this.provider = translateProvider(provider);
    this.uid = uid;
    this.token = token;
    updateGlobalUidFields();
  }

  public void setUid(String uid) {
    this.uid = uid;
    updateGlobalUidFields();
  }

  public void setProvider(String provider) {
    this.provider = translateProvider(provider);
    updateGlobalUidFields();
  }

  private String translateProvider(String provider) {
    return provider.toUpperCase();
  }

  public void setToken(String token) {
    this.token = token;
    updateGlobalUidFields();
  }

  protected void updateGlobalUidFields() {
    globalUid = createGlobalUid(provider, uid);
    globalUidAndToken = createGlobalUidAndToken(globalUid, token);
  }

  private static String createGlobalUid(String provider, String uid) {
    return uid + provider;
  }

  private static String createGlobalUidAndToken(String globalUid, String token) {
    return token + globalUid;
  }

  @Override
  public String toString() {
    return uid + "@" + provider + ":" + token;
  }

  public static Predicate<OAuthCredential> providerPredicate(
      final SocialNetworkProviderType provider) {
    return new Predicate<OAuthCredential>() {
      @Override
      public boolean apply(@Nullable OAuthCredential input) {
        return input.provider.equalsIgnoreCase(provider.toString());
      }
    };
  }
}
