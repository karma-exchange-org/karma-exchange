package org.karmaexchange.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import lombok.NoArgsConstructor;

import org.karmaexchange.dao.BaseDao;
import org.karmaexchange.resources.msg.BaseDaoView;

@NoArgsConstructor
public abstract class ViewlessBaseDaoResourceEx<T extends BaseDao<T> & BaseDaoView<T>>
    extends BaseDaoResourceEx<T, T> {

  @Override
  protected T createBaseDaoView(T obj) {
    return obj;
  }

  public ViewlessBaseDaoResourceEx(UriInfo uriInfo, Request request,
      ServletContext servletContext) {
    super(uriInfo, request, servletContext);
  }
}
