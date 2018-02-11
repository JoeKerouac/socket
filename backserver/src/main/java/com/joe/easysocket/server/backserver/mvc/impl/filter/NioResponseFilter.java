package com.joe.easysocket.server.backserver.mvc.impl.filter;


import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.ResponseContext;

/**
 * 响应filter
 *
 * @author joe
 */
public abstract class NioResponseFilter implements NioFilter {

    @Override
    public final void requestFilter(RequestContext.RequestWrapper request) {
        //加上final防止子类继承
    }

    @Override
    public abstract void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response);
}
