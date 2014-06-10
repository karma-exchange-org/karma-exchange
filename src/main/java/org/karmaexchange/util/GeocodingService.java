package org.karmaexchange.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.appengine.api.datastore.GeoPt;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.GeocoderStatus;

public class GeocodingService {

  public static final Level GEOCODE_LOG_LEVEL = Level.WARNING;
  private static final Logger log = Logger.getLogger(GeocodingService.class.getName());

  @Nullable
  public static GeoPt getGeoPt(String addressStr) {
    // TODO(avaliani): use an api key to avoid geocoding quota limits
    final Geocoder geocoder = new Geocoder();
    GeocoderRequest geocoderRequest = new GeocoderRequestBuilder()
      .setAddress(addressStr)
      .setLanguage("en")
      .getGeocoderRequest();
    GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);

    if (geocoderResponse.getStatus() == GeocoderStatus.OK) {
      GeocoderResult firstResult = geocoderResponse.getResults().get(0);
      return new GeoPt(
        firstResult.getGeometry().getLocation().getLat().floatValue(),
        firstResult.getGeometry().getLocation().getLng().floatValue());
    } else {
      log.log(GEOCODE_LOG_LEVEL,
        "Geocoding failed: status=" + geocoderResponse.getStatus() + ", " +
          "response=" + geocoderResponse);

      // TODO(avaliani): Properly handle geopt encoding failures. Retrying in cases where
      //   the error is over quota.
      return null;
    }
  }

}
