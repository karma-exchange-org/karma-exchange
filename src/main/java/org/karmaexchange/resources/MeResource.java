package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.OAuthCredential;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProviderFactory;
import org.karmaexchange.resources.msg.ResponseMsg;
import org.karmaexchange.resources.msg.ResponseMsg.ErrorMsg;

import com.googlecode.objectify.Key;

@Path("/me")
public class MeResource {

  @Context
  private UriInfo uriInfo;
  @Context
  private Request request;
  private OAuthCredential credential;

  public MeResource(@Context HttpServletRequest servletRequest) {
    credential = SocialNetworkProviderFactory.getLoginProviderCredential(servletRequest);
    if (credential == null) {
      throw ResponseMsg.createException("authentication credentials missing",
        ErrorMsg.Type.AUTHENTICATION);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getResource() {
    User user = getUser();
    if (user == null) {
      user = createUser();
    }
    return Response.ok(user).build();
  }

  private User getUser() {
    return ofy().load().type(User.class)
        .filter("oauthCredentials.globalUid", credential.getGlobalUid())
        .first()
        .now();
  }

  /**
   * Populate the user based upon information stored in the oAuth provider.
   */
  private User createUser() {
    User user = SocialNetworkProviderFactory.getProvider(credential).initUser();
    ofy().save().entity(user).now();
    return user;
  }

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response upsertResource(User updatedUser) {
    User currentUser = getUser();
    if (!currentUser.getId().equals(updatedUser.getId())) {
      throw ResponseMsg.createException(
        format("user id [%s] does not match current user id [%s]",
          updatedUser.getId(), currentUser.getId()),
        ErrorMsg.Type.BAD_REQUEST);
    }
    updateUser(currentUser, updatedUser, currentUser);
    return Response.ok().build();
  }

  void updateUser(User currentUser, User updatedUser, User oldUser) {
    if (updatedUser.getModificationInfo() == null) {
      throw ResponseMsg.createException("user.modificationInfo is null",
        ErrorMsg.Type.BAD_REQUEST);
    }
    updatedUser.getModificationInfo().update(currentUser);
    // Some fields can not be manipulated by updating the user.
    updatedUser.setKarmaPoints(oldUser.getKarmaPoints());
    updatedUser.setEventOrganizerRating(oldUser.getEventOrganizerRating());
    updatedUser.setOauthCredentials(oldUser.getOauthCredentials());
    ofy().save().entity(updatedUser).now();
  }

  @DELETE
  public void deleteResource() {
    User currentUser = getUser();
    if (currentUser != null) {
      ofy().delete().key(Key.create(currentUser)).now();
    }
  }
}
