package org.karmaexchange.resources;

import static java.lang.String.format;
import static org.karmaexchange.util.UserService.getCurrentUser;
import static org.karmaexchange.util.UserService.getCurrentUserKey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.User;
import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.EventSearchView;
import org.karmaexchange.resources.msg.ListResponseMsg;
import org.karmaexchange.resources.msg.OrganizationMembershipView;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.ImageUploadUtil;
import org.karmaexchange.util.UserService;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

@Path("/me")
public class MeResource {

  @Context
  private UriInfo uriInfo;
  @Context
  private Request request;

  public MeResource() {
    if (UserService.isNotLoggedInUser()) {
      throw ErrorResponseMsg.createException("Login required", ErrorInfo.Type.LOGIN_REQUIRED);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public User getResource() {
    return getCurrentUser();
  }

  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response updateResource(User updatedUser) {
    if (!updatedUser.isKeyComplete()) {
      updatedUser.initKey();
    }
    if (!getCurrentUserKey().getString().equals(updatedUser.getKey())) {
      throw ErrorResponseMsg.createException(
        format("thew new resource key [%s] does not match the previous key [%s]",
          getCurrentUserKey().getString(), updatedUser.getKey()),
        ErrorInfo.Type.BAD_REQUEST);
    }
    BaseDao.upsert(updatedUser);
    return Response.ok().build();
  }

  @DELETE
  public void deleteResource() {
    BaseDao.delete(getCurrentUserKey());
  }

  @Path("event")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<EventSearchView> getEvents() {
    return UserResource.userEventSearch(uriInfo, getCurrentUserKey());
  }

  @Path("org")
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public ListResponseMsg<OrganizationMembershipView> getOrgs() {
    return UserResource.getOrgs(getCurrentUserKey());
  }

  @Path("profile_image")
  @POST
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  public Response updateProfileImage(
      @QueryParam("provider") SocialNetworkProviderType providerType,
      @Context HttpServletRequest servletRequest) {
    if (providerType == null) {
      BlobKey blobKey = ImageUploadUtil.persistImage(servletRequest);
      try {
        User.updateProfileImage(getCurrentUserKey(), blobKey);
      } catch (RuntimeException e) {
        BlobstoreServiceFactory.getBlobstoreService().delete(blobKey);
        throw e;
      }
    } else {
      User.updateProfileImage(getCurrentUserKey(), providerType);
    }
    return Response.ok().build();
  }

  @Path("profile_image")
  @DELETE
  public void deleteProfileImage() {
    User.deleteProfileImage(getCurrentUserKey());
  }
}
