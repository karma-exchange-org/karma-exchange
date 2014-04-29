package org.karmaexchange.auth;

import static org.karmaexchange.util.OfyService.ofy;

import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.time.DateUtils;
import org.karmaexchange.dao.User;
import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;
import org.karmaexchange.util.ServletUtil;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
@Data
@NoArgsConstructor
public class Session {
  private static final int SESSION_EXPIRATION_HOURS = 24;
  private static final String SESSION_COOKIE_NAME = "session";

  // maxAge: 0 - means expire the cookie
  public static final NewCookie LOGOUT_COOKIE =
      new NewCookie(SESSION_COOKIE_NAME, "", "/", null, null, 0, false);

  @Id
  private String id;
  @Index
  private Date expiresOn;
  private Key<User> userKey;

  public Session(Key<User> userKey) {
    id = UUID.randomUUID().toString();
    expiresOn = DateUtils.addHours(new Date(), SESSION_EXPIRATION_HOURS);
  }

  public NewCookie getCookie() {
    // TODO(avaliani): In the future see which version of Jersey supports http only cookies.
    // maxAge: -1 - means session / browser lifetime cookie
    return new NewCookie(SESSION_COOKIE_NAME, id, "/", null, null, -1, false);
  }

  @Nullable
  public static Session getCurrentSession(HttpServletRequest req) {
    Cookie cookie = ServletUtil.getCookie(req, SESSION_COOKIE_NAME);
    if (cookie != null) {
      Session session = ofy().load().key(getKey(cookie)).now();
      if ((session == null) || session.isExpired()) {
        throw ErrorResponseMsg.createException("Session has expired",
          ErrorInfo.Type.AUTHENTICATION);
      }
      return session;
    }
    return null;
  }

  private static Key<Session> getKey(Cookie sessionCookie) {
    return Key.create(Session.class, sessionCookie.getValue());
  }

  private boolean isExpired() {
    return new Date().after(expiresOn);
  }
}
