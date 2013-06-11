package org.karmaexchange.dao;

import lombok.Data;

import org.karmaexchange.provider.SocialNetworkProvider.SocialNetworkProviderType;

import com.googlecode.objectify.annotation.Embed;

/**
 * This class provides a reference to a social network provider page.
 *
 * @author Amir Valiani (first.last@gmail.com)
 */
@Data
@Embed
public class PageRef {
  private String url;
  private SocialNetworkProviderType urlProvider;
}
