package org.karmaexchange.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
@NoArgsConstructor
@AllArgsConstructor
public final class Address {
  private String street;
  private String city;
  private String state;
  private String country;
  private String zip;
  private GeoPtWrapper geoPt;

  public String toGeocodeableString() {
    String result = "";
    result += getPartialGeocodeableString(street);
    result += getPartialGeocodeableString(city);
    result += getPartialGeocodeableString(state);
    result += getPartialGeocodeableString(zip);
    result += getPartialGeocodeableString(country);
    if (!result.isEmpty()) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private static String getPartialGeocodeableString(String addrEl) {
    if ((addrEl != null) && !addrEl.trim().isEmpty()) {
      return addrEl.trim() + ",";
    } else {
      return "";
    }
  }
}
