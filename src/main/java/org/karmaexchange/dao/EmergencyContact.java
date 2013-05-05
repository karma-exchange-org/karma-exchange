package org.karmaexchange.dao;

import lombok.Data;

import com.googlecode.objectify.annotation.Embed;

/**
*
* @author Amir Valiani
*/
@Data
@Embed
public final class EmergencyContact {
  private String name;
  private String phoneNumber;
  private String relationship;
}
