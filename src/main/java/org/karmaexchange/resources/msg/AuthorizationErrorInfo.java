package org.karmaexchange.resources.msg;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.util.OfyUtil;

import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class AuthorizationErrorInfo extends ErrorResponseMsg.ErrorInfo {

  @Nullable
  private String resourceKind;
  @Nullable
  private String resourceKey;

  public static WebApplicationException createException(BaseDao<?> resource) {
    return ErrorResponseMsg.createException(new AuthorizationErrorInfo(resource));
  }

  public static WebApplicationException createException(Key<?> resourceKey) {
    return ErrorResponseMsg.createException(new AuthorizationErrorInfo(resourceKey));
  }

  private AuthorizationErrorInfo(BaseDao<?> resource) {
    super("Authorization error", Type.NOT_AUTHORIZED);
    if (resource.isKeyComplete()) {
      resourceKey = Key.create(resource).getString();
    }
    resourceKind = OfyUtil.getKind(resource.getClass());
  }

  private AuthorizationErrorInfo(Key<?> resourceKey) {
    super("Authorization error", Type.NOT_AUTHORIZED);
    if ((resourceKey.getId() != 0) || (resourceKey.getName() != null)) {
      this.resourceKey = resourceKey.getString();
    }
    resourceKind = resourceKey.getKind();
  }

}
