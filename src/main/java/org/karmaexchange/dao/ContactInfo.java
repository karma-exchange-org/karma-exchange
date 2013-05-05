package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;

@Data
@Embed
public final class ContactInfo {

  // The email address is stored as is. Not lowercased. This field is not indexed.
  private String email;
  private String phoneNumber;
  private Address address;
}
