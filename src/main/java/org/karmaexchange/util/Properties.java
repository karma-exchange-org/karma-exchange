package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;

import lombok.Getter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

@ThreadSafe
public class Properties {

  private static final String CONFIG_FILE_PATH = "WEB-INF/app-private.properties";

  private static final String DOMAIN_PREFIX_SEPERATOR = "-";

  private static final ThreadLocal<String> host = new ThreadLocal<String>();

  @GuardedBy("Properties.class")
  private static PropertiesConfiguration config;

  public enum Property {
    FACEBOOK_APP_ID("facebook-app-id"),
    FACEBOOK_APP_SECRET("facebook-app-secret"),
    AJAX_SNAPSHOT_SERVICE_TOKEN("ajax-snapshots-snapshot-service-token", false),
    PRERENDER_SERVICE_TOKEN("prerender-snapshot-service-token", false);

    @Getter
    private final String propertyName;

    @Getter
    private boolean domainDependent;

    private Property(String propertyName) {
      this(propertyName, true);
    }

    private Property(String propertyName, boolean domainDependent) {
      this.propertyName = propertyName;
      this.domainDependent = domainDependent;
    }
  }

  private static synchronized PropertiesConfiguration getConfig() {
    if (config == null) {
      PropertiesConfiguration influxConfig = new PropertiesConfiguration();
      BufferedReader in;
      try {
        in = new BufferedReader(
          new InputStreamReader(new FileInputStream(CONFIG_FILE_PATH)));
      } catch (FileNotFoundException e) {
        throw new RuntimeException("Unabel to load configuration file " + CONFIG_FILE_PATH, e);
      }
      try {
        influxConfig.load(in);
      } catch (ConfigurationException e) {
        throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
      }
      config = influxConfig;
    }
    return config;
  }

  public static String get(Property property) {
    PropertiesConfiguration propConfig = getConfig();
    String value;
    if (property.isDomainDependent()) {
      if (host.get() == null) {
        throw new RuntimeException("Initialization error: init must be invoked");
      }
      value = propConfig.getString(getDomainPropertyName(property));
      checkState(value != null,
        format("unable to find property '%s' in '%s'", getDomainPropertyName(property),
          CONFIG_FILE_PATH));
      return value;
    } else {
      value = propConfig.getString(property.getPropertyName());
      checkState(value != null,
          format("unable to find property '%s' in '%s'", property.getPropertyName(),
            CONFIG_FILE_PATH));
      return value;
    }
  }

  private static String getDomainPropertyName(Property property) {
    return host.get() + DOMAIN_PREFIX_SEPERATOR + property.getPropertyName();
  }

  /**
   * Must be invoked prior to invoking any code that fetches properties.
   */
  static void requestStart(HttpServletRequest req) {
    try {
      host.set(
        new URL(req.getRequestURL().toString()).getHost());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Invoked to cleanup state from {@link #requestStart}.
   */
  static void requestEnd() {
    host.set(null);
  }
}
