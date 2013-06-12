package org.karmaexchange.dao;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import com.googlecode.objectify.annotation.Embed;

/**
 * This class provides a reference to a social network provider page.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
@Data
@NoArgsConstructor
@Embed
public class PageRef {
  private String url;
  private SocialNetworkProviderType urlProvider;

  public static PageRef create(String url, SocialNetworkProviderType urlProvider) {
    return new PageRef(url, urlProvider);
  }

  public PageRef(String url, SocialNetworkProviderType urlProvider) {
    this.url = url;
    this.urlProvider = urlProvider;
  }
}
