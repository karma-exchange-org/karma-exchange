package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.auth.AuthProvider;
import org.karmaexchange.auth.AuthProvider.UserInfo;
import org.karmaexchange.auth.AuthProviderCredentials;
import org.karmaexchange.auth.AuthProviderType;
import org.karmaexchange.auth.GlobalUid;
import org.karmaexchange.auth.GlobalUidMapping;
import org.karmaexchange.auth.Session;
import org.karmaexchange.dao.KeyWrapper;
import org.karmaexchange.dao.User;
import org.karmaexchange.dao.UserUsage;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Path("/auth")
@NoArgsConstructor
public class AuthResource {

  @Path("login")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response login(LoginRequest loginRequest) {
    // 1. Verify the credentials
    AuthProvider authProvider =
        loginRequest.getProviderTyoe().getProvider();
    GlobalUid globalUid =
        authProvider.verifyUserCredentials(loginRequest.getCredentials());

    // 2. Lookup the user based on the credentials.
    Key<GlobalUidMapping> userMappingKey = GlobalUidMapping.getKey(globalUid);
    GlobalUidMapping userMapping =
        ofy().load().key(userMappingKey).now();
    User user;
    if (userMapping == null) {
      // If a user doesn't exist create one.
      user = createUserAndMapping(authProvider, loginRequest.getCredentials(), globalUid);
    } else {
      user = ofy().load().key(userMapping.getUserKey()).now();
    }

    // 3. Create a new session for the user.
    Session session = new Session(Key.create(user));
    ofy().save().entity(session);  // Asynchronously save the session.

    // 4. Track access.
    UserUsage.trackAccess(userMappingKey, user);

    // 5. Return the login response.
    return Response.ok(new LoginResponse(user))
        .cookie(session.getCookie())
        .build();
  }

  @Path("logout")
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response logout() {
    return Response.ok()
        .cookie(Session.LOGOUT_COOKIE)
        .build();
  }

  private User createUserAndMapping(AuthProvider authProvider, AuthProviderCredentials credentials,
      GlobalUid globalUid) {
    UserInfo userInfo = authProvider.createUser(credentials);
    User.persistNewUser(userInfo);

    // TODO(avaliani): handle orphaned user objects. Until there is a mapping to the user
    //   the user object is not retrievable.

    User user = userInfo.getUser();
    GlobalUidMapping mapping = new GlobalUidMapping(globalUid, Key.create(user));
    ofy().save().entity(mapping);  // Asynchronously save the new mapping

    return user;
  }

  @XmlRootElement
  @Data
  private static class LoginRequest {
    private AuthProviderType providerTyoe;
    private AuthProviderCredentials credentials;
  }

  @XmlRootElement
  @Data
  @NoArgsConstructor
  private static class LoginResponse {

    private KeyWrapper<User> userKey;
    private String firstName;
    private String lastName;
    private String email;

    public LoginResponse(User user) {
      userKey = KeyWrapper.create(user);
      firstName = user.getFirstName();
      lastName = user.getLastName();
      email = user.getPrimaryEmail();
    }
  }
}
