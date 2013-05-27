package org.karmaexchange.dao;

import static org.karmaexchange.util.JsonValidationTestUtil.validateJsonConversion;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.junit.Before;
import org.junit.Test;
import org.karmaexchange.util.DatastoreTestUtil;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Parent;

public class InheritenceTest extends PersistenceTestHelper {

  @Data
  public static class RootDao {
    @Parent
    private Key<?> owner;
    @Id private Long id;
    @Ignore
    private String key;
    private ModificationInfo modificationInfo;

    public void setOwner(String keyStr) {
      owner = Key.<Object>create(keyStr);
    }

    public String getOwner() {
      return (owner == null) ? null : owner.getString();
    }
  }

  @XmlRootElement
  @Entity
  @Data
  @EqualsAndHashCode(callSuper=true)
  @ToString(callSuper=true)
  public static class EventX extends RootDao {
    private String title;
  }

  private EventX event;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ObjectifyService.register(EventX.class);
    event = new EventX();
    event.setTitle("my title");
    event.setId((long) 1);
    event.setKey("xyz");
    ModificationInfo mi = new ModificationInfo();
    mi.setCreationDate(new Date());
    event.setModificationInfo(mi);
    event.setOwner(Key.create(EventX.class, 2).getString());
  }

  @Test
  public void testJsonConversion() throws Exception {
    validateJsonConversion(event, EventX.class);
  }

  @Test
  public void testPersistence() throws Exception {
    validatePersistence(event);
    DatastoreTestUtil.dumpEntity(event);
  }
}
