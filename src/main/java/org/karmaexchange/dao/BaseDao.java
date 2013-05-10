package org.karmaexchange.dao;

import static java.lang.String.format;
import static org.karmaexchange.util.OfyService.ofy;

import java.util.List;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

public abstract class BaseDao<T extends BaseDao<T>> {

  public abstract void setId(Long id);
  public abstract Long getId();

  public abstract String getKey();
  public abstract void setKey(String key);

  public abstract ModificationInfo getModificationInfo();
  public abstract void setModificationInfo(ModificationInfo modificationInfo);

  public static <T extends BaseDao<T>> void upsert(T resource, User updateUser) {
    // Cleanup any id and key mismatch.
    if ((resource.getId() == null) && (resource.getKey() != null)) {
      resource.setId(Key.<T>create(resource.getKey()).getId());
    }
    if (resource.getId() != null) {
      if (resource.getKey() == null) {
        resource.updateKey();
      } else if (!resource.getKey().equals(Key.create(resource).getString())) {
        throw ErrorResponseMsg.createException(
          format("resource key [%s] does not match id based key [%s]",
            resource.getKey(), Key.create(resource).getString()),
          ErrorInfo.Type.BAD_REQUEST);
      }
    }

    T prevResource = null;
    if (resource.getId() != null) {
      prevResource = load(Key.create(resource));
    }
    if (prevResource == null) {
      resource.insert();
    } else {
      resource.update(prevResource, updateUser);
    }
  }

  public static <T extends BaseDao<T>> T load(String keyStr) {
    return load(Key.<T>create(keyStr));
  }

  public static <T extends BaseDao<T>> T load(Key<T> key) {
    T resource = ofy().load().key(key).get();
    if (resource != null) {
      resource.processLoad();
    }
    return resource;
  }

  public static <T extends BaseDao<T>> List<T> loadAll(Class<T> resourceClass) {
    List<T> resources = ofy().load().type(resourceClass).list();
    for (T resource : resources) {
      resource.processLoad();
    }
    return resources;
  }

  public static <T extends BaseDao<T>> T loadFirst(Query<T> query) {
    T resource = query.first().now();
    if (resource != null) {
      resource.processLoad();
    }
    return resource;
  }

  void insert() {
    ofy().save().entity(this).now();
    updateKey();
  }

  public final void update(T prevObj, User updateUser) {
    processUpdate(prevObj, updateUser);
    ofy().save().entity(this).now();
  }

  public void delete() {
    ofy().delete().key(Key.create(this)).now();
  }

  protected void processUpdate(T prevObj, User updateUser) {
    updateKey();
    if (!prevObj.getKey().equals(getKey())) {
      throw ErrorResponseMsg.createException(
        format("thew new resource key [%s] does not match the previous key [%s]",
          getKey(), prevObj.getKey()),
        ErrorInfo.Type.BAD_REQUEST);
    }
    if (getModificationInfo() == null) {
      // Handle objects that were created without modification info.
      if (prevObj.getModificationInfo() == null) {
        setModificationInfo(ModificationInfo.create(updateUser));
      } else {
        setModificationInfo(prevObj.getModificationInfo());
      }
    }
    getModificationInfo().update(updateUser);
  }

  protected void processLoad() {
    updateKey();
  }

  protected void updateKey() {
    setKey(KeyWrapper.create(this).getKey());
  }
}
