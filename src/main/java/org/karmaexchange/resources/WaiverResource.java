package org.karmaexchange.resources;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.karmaexchange.dao.Waiver;
import org.karmaexchange.resources.msg.WaiverView;

@Path("/waiver")
public class WaiverResource extends BaseDaoResourceEx<Waiver, WaiverView> {

  public static final String PATH = "waiver";

  @Override
  protected WaiverView createBaseDaoView(Waiver waiver) {
    return new WaiverView(waiver);
  }

  @Override
  public Response upsertResource(WaiverView waiverView) {
    throw new UnsupportedOperationException("untethered creation of waivers is not supported");
  }
}
