package org.karmaexchange.snapshot;

import static org.karmaexchange.util.Properties.Property.AJAX_SNAPSHOT_SERVICE_TOKEN;

import org.karmaexchange.util.Properties;

import com.github.avaliani.snapshot.SnapshotServiceTokenProvider;

public class AjaxSnapshotsServiceTokenProvider implements SnapshotServiceTokenProvider {

  @Override
  public String getServiceToken() {
    return Properties.get(AJAX_SNAPSHOT_SERVICE_TOKEN);
  }

}
