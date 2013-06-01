package org.karmaexchange.dao;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Index;

@Embed
@EqualsAndHashCode
public class OAuthCredential {
  @Getter
  private String uid;
  @Getter
  private String provider;
  @Getter
  private String token;

  @Getter
  private String globalUid;
  @Index
  @Getter
  private String globalUidAndToken;

  public static OAuthCredential create(String provider, String uid, String token) {
    OAuthCredential credential = new OAuthCredential();
    credential.setProvider(provider);
    credential.setUid(uid);
    credential.setToken(token);
    return credential;
  }

  public void setUid(String uid) {
    this.uid = uid;
    updateIndexedFields();
  }

  public void setProvider(String provider) {
    this.provider = provider;
    updateIndexedFields();
  }

  public void setToken(String token) {
    this.token = token;
    updateIndexedFields();
  }

  private void updateIndexedFields() {
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
}
