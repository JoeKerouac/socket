package com.joe.easysocket.server.backserver.mvc.impl.filter;


import com.joe.easysocket.server.backserver.mvc.impl.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.ResponseContext;

/**
 * filter接口，用户必须实现该接口并且加上{@link com.joe.easysocket.server.backserver.mvc.impl.container.Provider}注解
 *
 * @author joe
 */
public interface NioFilter extends Bean {
    /**
     * 请求filter
     *
     * @param request 请求上下文
     */
    void requestFilter(RequestContext.RequestWrapper request);

    /**
     * 响应filter
     *
     * @param request  请求上下文
     * @param response 响应上下文
     */
    void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response);
}
