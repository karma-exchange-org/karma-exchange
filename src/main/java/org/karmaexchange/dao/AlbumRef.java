package org.karmaexchange.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumRef {
  private String id;
  private ImageProviderType urlProvider;
}
