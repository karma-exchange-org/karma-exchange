package org.karmaexchange.snapshot;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.codec.digest.DigestUtils;

import com.github.avaliani.snapshot.SnapshotResult;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Data
@NoArgsConstructor
public class WebPageSnapshot {

  private String url;
  @Id
  private String urlMd5;
  private String snapshot;
  private Date creationDate;

  public WebPageSnapshot(HttpServletRequest clientRequest, SnapshotResult result) {
    url = getCanonicalUrl(clientRequest);
    urlMd5 = DigestUtils.md5Hex(url);
    snapshot = result.getSnapshot();
    creationDate = new Date();
  }

  public SnapshotResult toSnapshotResult() {
    return new SnapshotResult(
      snapshot,
      Maps.<String, List<String>>newHashMap());
  }

  public static Key<WebPageSnapshot> getKey(HttpServletRequest clientRequest) {
    return Key.create(
      WebPageSnapshot.class,
      DigestUtils.md5Hex(getCanonicalUrl(clientRequest)));
  }

  public static String getCanonicalUrl(HttpServletRequest request) {
    String requestUrl = request.getRequestURL().toString();
    String queryString = request.getQueryString();
    if (queryString != null) {
      requestUrl += "?" + queryString;
    }
    // TODO(avaliani): consider stripping some of the non-essential parameters.
    return requestUrl;
  }
}
