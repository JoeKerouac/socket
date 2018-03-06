package com.joe.easysocket.server.backserver.mvc.impl.filter;


import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpResponseContext;

/**
 * filter接口，用户必须实现该接口并且加上{@link Provider}注解
 *
 * @author joe
 */
public interface NioFilter extends Bean {
    /**
     * 请求filter
     *
     * @param request 请求上下文
     */
    void requestFilter(HttpRequestContext.RequestWrapper request);

    /**
     * 响应filter
     *
     * @param request  请求上下文
     * @param response 响应上下文
     */
    void responseFilter(HttpRequestContext.RequestWrapper request, HttpResponseContext.Response response);
}
