package org.karmaexchange.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.ServletContext;

import lombok.AccessLevel;
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

    private final String propertyName;

    private Property(String propertyName) {
      this.propertyName = propertyName;
    }

    private String getPropertyName() {
      if (isProductionDeployment()) {
        return propertyName;
      } else {
        return TEST_PROPERTY_PREFIX + propertyName;
      }
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
      configToInit.setThrowExceptionOnMissing(true);
      config = configToInit;
    }
    return config;
  }

  public static String get(ServletContext context, Property property) {
    // TODO(avaliani): low-pri: see if I can optimize the unconditional lock on getConfig.
    return getConfig(context).getString(property.getPropertyName());
  }

  private static boolean isProductionDeployment() {
    return SystemProperty.environment.value() == SystemProperty.Environment.Value.Production;
  }
}
