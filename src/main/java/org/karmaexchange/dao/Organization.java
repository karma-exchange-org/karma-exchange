package org.karmaexchange.dao;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

// import org.codehaus.jackson.annotate.JsonTypeInfo;

import lombok.Data;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
// TODO(avaliani): revisit polymorphic typing.
// Note: this is now working w/ POJO model. Need to just fix the test cases.
// @JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
//
// @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="_type")
// @JsonSubTypes({
//     @Type(value=NonProfitOrganization.class, name="NonProfitOrganization")})
public class Organization {

  /*
   * TODO(avaliani): revisit polymorphic typing.
   *
   * | public enum Type {
   * |   NON_PROFIT,
   * |   COMMERCIAL
   * | }
   * | private Type type;
  */

  @Id private Long id;
  private ModificationInfo modificationInfo;

  @Index
  private String name;
  private String about;
  private String website;
  private Image displayImage;
  private ContactInfo contactInfo;

  private List<KeyWrapper<Cause>> causes;

  private List<KeyWrapper<User>> admins;

  @Index
  private long karmaPoints;

  private Rating eventRating;
}
