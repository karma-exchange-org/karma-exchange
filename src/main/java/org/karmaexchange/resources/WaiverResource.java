package org.karmaexchange.resources;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.Waiver;

@Path("/waiver")
public class WaiverResource extends ViewlessBaseDaoResource<Waiver> {

  public static final String PATH = "waiver";

  @Override
  public Response upsertResource(Waiver waiver) {
    throw new UnsupportedOperationException("untethered creation of waivers is not supported");
  }
}
