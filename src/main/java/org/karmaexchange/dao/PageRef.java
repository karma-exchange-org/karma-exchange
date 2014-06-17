package org.karmaexchange.dao;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

/**
 * This class provides a reference to a social network provider page.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
@Data
@NoArgsConstructor
public class PageRef {

  private String name;
  private String url;
  private SocialNetworkProviderType urlProvider;

  public static PageRef create(String name, String url, SocialNetworkProviderType urlProvider) {
    return new PageRef(name, url, urlProvider);
  }

  public PageRef(String name, String url, SocialNetworkProviderType urlProvider) {
    this.name = name;
    this.url = url;
    this.urlProvider = urlProvider;
  }

  public boolean isValid() {
    return (name != null) && (urlProvider != null) && (url != null);
  }

}
