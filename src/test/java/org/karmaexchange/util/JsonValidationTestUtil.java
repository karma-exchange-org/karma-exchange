package org.karmaexchange.util;

import static org.junit.Assert.assertEquals;
import static org.karmaexchange.util.TestUtil.DEBUG;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.json.JSONUnmarshaller;

public final class JsonValidationTestUtil {

  public static <T> void validateJsonConversion(T entity, Class<T> entityClass)
      throws JAXBException {
    JSONJAXBContext jsonContext = new JSONJAXBContext(
      JSONConfiguration.natural().humanReadableFormatting(true).build(),
      entityClass);

    JSONMarshaller jsonMarshaller = jsonContext.createJSONMarshaller();
    StringWriter jsonEntityStringWriter = new StringWriter();
    jsonMarshaller.marshallToJSON(entity, jsonEntityStringWriter);
    String jsonEntityStr = jsonEntityStringWriter.toString();

    if (DEBUG) {
      System.out.println("Json:\n" + jsonEntityStr);
    }

    JSONUnmarshaller jsonUnmarshaller = jsonContext.createJSONUnmarshaller();
    T unmarshalledEntity = jsonUnmarshaller.unmarshalFromJSON(
      new StringReader(jsonEntityStr),
      entityClass);

    if (DEBUG) {
      System.out.println(entity);
    }

    assertEquals(entity, unmarshalledEntity);
  }
}
