package org.karmaexchange.dao;

import javax.xml.bind.annotation.XmlRootElement;

import com.googlecode.objectify.annotation.EntitySubclass;

/*
 * TODO(avaliani): revisit polymorphic typing.
 *
 * See: http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 */
@XmlRootElement
@EntitySubclass(index=true)
// @JsonTypeName("NonProfitOrganization")
public final class NonProfitOrganization extends Organization {

}
