package org.karmaexchange.resources;

import static org.karmaexchange.util.OfyService.ofy;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.Organization;
import org.karmaexchange.dao.derived.EventSourceInfo;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;

@Path(EventSourceInfoResource.RESOURCE_PATH)
public class EventSourceInfoResource {

  public static final String RESOURCE_PATH = "/admin/event_source_info";

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public EventSourceInfo getResource(
      @QueryParam("orgId") String orgId) {
    Key<Organization> orgKey = validateOrgId(orgId);
    return ofy().load()
        .key(EventSourceInfo.createKey(orgKey))
        .now();
  }

  @POST
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void updateResource(@QueryParam("orgId") String orgId,
      EventSourceInfo inputSourceInfo) {
    Key<Organization> orgKey = validateOrgId(orgId);
    EventSourceInfo sourceInfoToSave =
        new EventSourceInfo(orgKey, inputSourceInfo.getSecret(), inputSourceInfo.getDomain());
    BaseDao.upsert(sourceInfoToSave);
  }

  private Key<Organization> validateOrgId(String orgId) {
    if (orgId == null) {
      throw ErrorResponseMsg.createException(
        "'orgId' must be specified",
        ErrorInfo.Type.BAD_REQUEST);
    }
    return Organization.createKey(orgId);
  }
}
