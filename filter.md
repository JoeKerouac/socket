# filter使用
后台[backserver](backserver)内置的[MvcDataworker](backserver/src/main/java/com/joe/easysocket/server/backserver/impl/MvcDataworker)支持filter操作（就像SpringMVC中一样，不过支持没有SpringMVC的强大），使用时只需要继承NioRequestFilter或者NioResponseFilter或者NioFilter，然后在类上添加Provider注解即可。

RequestFilter示例：
```
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
    }
}
```
ResponseFilter示例：
```
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
```

**注意：当前RequestFilter暂时不支持提前中断请求，仅仅可以用来记录请求内容、计算请求时间等**

[下一节：UDP支持](udp-supported.md)