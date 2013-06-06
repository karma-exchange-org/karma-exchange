package org.karmaexchange.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.dao.CauseType;

@Path("/cause_type")
public class CauseTypeResource extends BaseDaoResource<CauseType> {

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public List<CauseType> getResources() {
    return BaseDao.loadAll(getResourceClass());
  }

  @Override
  protected Class<CauseType> getResourceClass() {
    return CauseType.class;
  }
}
