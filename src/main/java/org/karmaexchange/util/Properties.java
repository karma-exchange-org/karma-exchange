package org.karmaexchange.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.ServletContext;

import lombok.Getter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.google.appengine.api.utils.SystemProperty;

@ThreadSafe
public class Properties {

  private static final String CONFIG_FILE_PATH = "/WEB-INF/server_properties.txt";
  private static final String TEST_PROPERTY_PREFIX = "test-";

  @GuardedBy("Properties.class")
  private static PropertiesConfiguration config;

  public enum Property {
    FACEBOOK_APP_SECRET("facebook-app-secret");

    @Getter
    private final String propertyName;

    private Property(String propertyName) {
      this.propertyName = propertyName;
    }

    public boolean testAndProductionDistinct() {
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

  public static String get(ServletContext context, Property property) {
    PropertiesConfiguration propConfig = getConfig(context);
    String value;
    if (!isProductionDeployment()) {
      value = propConfig.getString(getTestPropertyName(property));
      if (value != null) {
        return value;
      }
      if (property.testAndProductionDistinct()) {
        throw new NullPointerException(
          format("property '%s' not specified in '%s'", getTestPropertyName(property),
            CONFIG_FILE_PATH));
      }
    }
    value = propConfig.getString(property.getPropertyName());
    return checkNotNull(value,
      format("property '%s' not specified in '%s'", property.getPropertyName(), CONFIG_FILE_PATH));
  }

  private static String getTestPropertyName(Property property) {
    return TEST_PROPERTY_PREFIX + property.getPropertyName();
  }

  private static boolean isProductionDeployment() {
    return SystemProperty.environment.value() == SystemProperty.Environment.Value.Production;
  }
}
