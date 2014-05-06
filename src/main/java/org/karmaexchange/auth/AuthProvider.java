package org.karmaexchange.auth;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.karmaexchange.dao.ImageProviderType;
import org.karmaexchange.dao.User;


public interface AuthProvider {

  CredentialVerificationResult verifyUserCredentials(AuthProviderCredentials userCredentials,
      HttpServletRequest req);

  UserInfo createUser(CredentialVerificationResult verificatonResult);

  @Data
  @AllArgsConstructor
  public static class UserInfo {
    private final User user;

    @Nullable
    private final ProfileImage profileImage;

    public UserInfo(User user) {
      this(user, null);
    }

    @Data
    public static class ProfileImage {
      private final ImageProviderType provider;
      private final String url;
    }
  }

  @Data
  public static class CredentialVerificationResult {
    private final GlobalUid globalUid;
    private final CredentialVerificationCtx verificationCtx;
  }

  public interface CredentialVerificationCtx {
  }
}
