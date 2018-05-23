package com.joe.test;

import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpResponseContext;
import com.joe.easysocket.server.backserver.mvc.impl.filter.NioResponseFilter;

/**
 * 响应filter
 *
 * @author joe
 * @version 2018.05.23 15:51
 */
@Provider
public class ResponseFilterTest extends NioResponseFilter {
    @Override
    public void responseFilter(HttpRequestContext.RequestWrapper request, HttpResponseContext.Response response) {
        System.out.println("响应数据为：" + response.getResult());
    }
}
