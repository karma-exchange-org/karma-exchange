package org.karmaexchange.provider;

import javax.servlet.http.HttpServletRequest;

import lombok.Data;

import info.modprobe.browserid.BrowserIDException;
import info.modprobe.browserid.BrowserIDResponse;
import info.modprobe.browserid.BrowserIDResponse.Status;
import info.modprobe.browserid.Verifier;

import org.karmaexchange.auth.AuthProvider;
import org.karmaexchange.auth.AuthProviderCredentials;
import org.karmaexchange.auth.AuthProviderType;
import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.User.RegisteredEmail;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.ServletUtil;

public class MozillaPersonaAuthProvider implements AuthProvider {

  @Override
  public CredentialVerificationResult verifyUserCredentials(AuthProviderCredentials userCredentials,
      HttpServletRequest req) {
    Verifier verifier = new Verifier();
    BrowserIDResponse loginResponse;
    try {
      loginResponse = verifier.verify(userCredentials.getToken(),
        ServletUtil.getBaseUriWithPort(req));
    } catch (BrowserIDException e) {
      throw ErrorResponseMsg.createException(e.getMessage(),
        ErrorInfo.Type.PARTNER_SERVICE_FAILURE);
    }
    Status status = loginResponse.getStatus();
    if (status == Status.OK) {
      return new CredentialVerificationResult(
        GlobalUid.create(AuthProviderType.MOZILLA_PERSONA, loginResponse.getEmail()),
        new MozillaPersonaCredentialVerificationCtx(loginResponse.getEmail()));
    } else {
      throw ErrorResponseMsg.createException(loginResponse.getReason(),
        ErrorInfo.Type.AUTHENTICATION);
    }
  }

  @Override
  public UserInfo createUser(CredentialVerificationResult verificationResult) {
    MozillaPersonaCredentialVerificationCtx ctx =
        (MozillaPersonaCredentialVerificationCtx) verificationResult.getVerificationCtx();

    User user = User.create();
    user.getRegisteredEmails().add(new RegisteredEmail(ctx.getEmail(), true));
    return new UserInfo(user);
  }

  @Data
  private static class MozillaPersonaCredentialVerificationCtx
      implements CredentialVerificationCtx {
    private final String email;
  }
}
