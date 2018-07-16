package com.joe.easysocket.server.backserver.mvc.impl.filter;

import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpResponseContext;

/**
 * 响应filter
 *
 * @author joe
 */
public abstract class NioResponseFilter implements NioFilter {

    @Override
    public final void requestFilter(HttpRequestContext.RequestWrapper request) {
        //加上final防止子类继承
    }

    @Override
    public abstract void responseFilter(HttpRequestContext.RequestWrapper request,
                                        HttpResponseContext.Response response);
}
