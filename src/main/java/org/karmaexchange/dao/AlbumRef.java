package org.karmaexchange.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.googlecode.objectify.annotation.Embed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embed
public class AlbumRef {
  private String id;
  private ImageProviderType urlProvider;
}
