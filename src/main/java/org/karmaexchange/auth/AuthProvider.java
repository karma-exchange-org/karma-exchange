package org.karmaexchange.auth;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.karmaexchange.dao.ImageProviderType;
import org.karmaexchange.dao.User;


public interface AuthProvider {

  GlobalUid verifyUserCredentials(AuthProviderCredentials userCredentials);

  UserInfo createUser(AuthProviderCredentials userCredentials);

  @Data
  @AllArgsConstructor
  public static class UserInfo {
    private User user;

    @Nullable
    private ImageProviderType profileImageProvider;
    @Nullable
    private String profileImageUrl;
  }
}
