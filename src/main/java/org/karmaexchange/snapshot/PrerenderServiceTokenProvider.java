package org.karmaexchange.snapshot;

import static org.karmaexchange.util.Properties.Property.PRERENDER_SERVICE_TOKEN;

import org.karmaexchange.util.Properties;

import com.github.avaliani.snapshot.SnapshotServiceTokenProvider;

public class PrerenderServiceTokenProvider implements SnapshotServiceTokenProvider {

  @Override
  public String getServiceToken() {
    return Properties.get(PRERENDER_SERVICE_TOKEN);
  }

}
