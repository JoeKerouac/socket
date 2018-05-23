package com.joe.test;

import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.filter.NioRequestFilter;

/**
 * 请求filter
 *
 * @author joe
 * @version 2018.05.23 15:49
 */
@Provider
public class RequestFilterTest extends NioRequestFilter{
    @Override
    public void requestFilter(HttpRequestContext.RequestWrapper request) {
        System.out.println("用户IP为：" + request.getSession().getRemoteHost());
        System.out.println("用户端口为：" + request.getSession().getRemotePort());
        throw new RuntimeException("中断请求");
    }
}
