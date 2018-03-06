package com.joe.easysocket.server.backserver.mvc.impl.filter;


import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpResponseContext;

/**
 * 请求filter
 *
 * @author joe
 */
public abstract class NioRequestFilter implements NioFilter {

    @Override
    public abstract void requestFilter(HttpRequestContext.RequestWrapper request);

    @Override
    public final void responseFilter(HttpRequestContext.RequestWrapper request, HttpResponseContext.Response response) {
        //加上final防止子类继承
    }

}
