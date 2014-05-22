package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.auth.AuthProvider;
import org.karmaexchange.auth.AuthProvider.CredentialVerificationResult;
import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.auth.AuthProviderCredentials;
import org.karmaexchange.auth.AuthProviderType;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.Session;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserUsage;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Path(AuthResource.RESOURCE_PATH)
@NoArgsConstructor
public class AuthResource {

  /*
   * All methods in this class are invoked with the admin user key. This enables us to
   * create and load any user object.
   */

  public static final String RESOURCE_PATH = "/auth";

  @Path("login")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response login(LoginRequest loginRequest,
      @Context HttpServletRequest req) {
    // 1. Verify the credentials
    AuthProvider authProvider =
        loginRequest.getProviderType().getProvider();
    CredentialVerificationResult verificationResult =
        authProvider.verifyUserCredentials(loginRequest.getCredentials(), req);

    // 2. Lookup the user based on the credentials.
    GlobalUidMapping userMapping = GlobalUidMapping.load(verificationResult.getGlobalUid());
    User user;
    if (userMapping == null) {
      // If a user doesn't exist create one.
      user = createUserAndMapping(authProvider, verificationResult);
    } else {
      user = ofy().load().key(userMapping.getUserKey()).now();
      if (user == null) {
        // It's possible that the user has been deleted. If so, create a new user.
        user = createUserAndMapping(authProvider, verificationResult);
      }
    }

    // 3. Create a new session for the user.
    Session session = new Session(Key.create(user));
    ofy().save().entity(session);  // Asynchronously save the session.

    // 4. Track access.
    UserUsage.trackAccess(verificationResult.getGlobalUid(), user);

    // 5. Return the login response.
    return Response.ok(user)
        .cookie(session.getCookie())
        .build();
  }

  /*
   * Extends an existing non-expired session by creating a new session with the default
   * number of hours before it expires.
   *
   * Renew is a quick workaround to implementing the full session expiration logic. Sessions
   * are renewed the first time the app is loaded. App load time is an easy time to renew because
   * there is no concern about in-flight requests. The app still has to handle sessions that
   * are expired when the app is loaded.
   */
  @Path("renew")
  @POST
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response renew(@Context HttpServletRequest req) {
    Session oldSession = Session.getCurrentSession(req);
    // Handle the case where there is no cookie.
    if (oldSession == null) {
      throw ErrorResponseMsg.createException("Session has expired",
        ErrorInfo.Type.SESSION_EXPIRED);
    }

    User user = ofy().load().key(oldSession.getUserKey()).now();
    if (user == null) {
      throw ErrorResponseMsg.createException("User no longer exists for session",
        ErrorInfo.Type.SESSION_EXPIRED);
    }

    // Create the new session
    Session newSession = new Session(oldSession.getUserKey());
    ofy().save().entity(newSession);  // Asynchronously save the session.

    // Delete the old session
    ofy().delete().entity(oldSession);

    return Response.ok(user)
        .cookie(newSession.getCookie())
        .build();
  }


  @Path("logout")
  @POST
  public Response logout(@Context HttpServletRequest req) {
    // Delete the key so it can not be used by anyone else.
    Key<Session> sessionKey = Session.getCurrentSessionKey(req);
    if (sessionKey != null) {
      ofy().delete().key(sessionKey);
    }

    return Response.ok()
        .cookie(Session.LOGOUT_COOKIE)
        .build();
  }

  private User createUserAndMapping(AuthProvider authProvider,
      CredentialVerificationResult verificationResult) {
    UserInfo userInfo = authProvider.createUser(verificationResult);
    Key<User> userKey = User.upsertNewUser(userInfo);

    // Now that the user has been persisted, get the persisted version of the user.
    User user = ofy().load().key(userKey).now();

    // TODO(avaliani): handle orphaned user objects. Until there is a mapping to the user
    //   the user object is not retrievable.

    GlobalUidMapping mapping =
        new GlobalUidMapping(verificationResult.getGlobalUid(), Key.create(user));
    ofy().save().entity(mapping);  // Asynchronously save the new mapping

    return user;
  }

  @XmlRootElement
  @Data
  private static class LoginRequest {
    private AuthProviderType providerType;
    private AuthProviderCredentials credentials;
  }

}
