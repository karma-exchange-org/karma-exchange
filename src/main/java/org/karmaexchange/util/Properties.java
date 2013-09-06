package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.ServletContext;

import lombok.Getter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

@ThreadSafe
public class Properties {

  private static final String CONFIG_FILE_PATH = "/WEB-INF/app-private.properties";

  private static final String DOMAIN_PREFIX_SEPERATOR = "-";

  @GuardedBy("Properties.class")
  private static PropertiesConfiguration config;

  public enum Property {
    FACEBOOK_APP_ID("facebook-app-id"),
    FACEBOOK_APP_SECRET("facebook-app-secret");

    @Getter
    private final String propertyName;

    private Property(String propertyName) {
      this.propertyName = propertyName;
    }

    public boolean isDomainDependent() {
      return true;
    }
  }

  private static synchronized PropertiesConfiguration getConfig(ServletContext context) {
    if (config == null) {
      PropertiesConfiguration configToInit = new PropertiesConfiguration();
      BufferedReader in = new BufferedReader(
        new InputStreamReader(context.getResourceAsStream(CONFIG_FILE_PATH)));
      try {
        configToInit.load(in);
      } catch (ConfigurationException e) {
        throw ErrorResponseMsg.createException(e, ErrorInfo.Type.BACKEND_SERVICE_FAILURE);
      }
      config = configToInit;
    }
    return config;
  }

  public static String get(ServletContext context, URI requestUri, Property property) {
    PropertiesConfiguration propConfig = getConfig(context);
    String value;
    if (property.isDomainDependent()) {
      value = propConfig.getString(getDomainPropertyName(property, requestUri));
      checkState(value != null,
        format("unable to find property '%s' in '%s'", getDomainPropertyName(property, requestUri),
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

  private static String getDomainPropertyName(Property property, URI requestUri) {
    return requestUri.getHost() + DOMAIN_PREFIX_SEPERATOR + property.getPropertyName();
  }
}
