# 配置
Balance可以通过[Config](balance/src/main/java/com/joe/easysocket/server/balance/Config.java)来配置，而BackServer则是可以通过[Config](backserver/src/main/java/com/joe/easysocket/server/backserver/Config.java)来配置
# 默认MvcDataworker使用：
## 数据
MvcDataworker数据报的类型（type）为1，body的序列化方式为JSON，详情如下：
```json
{
    "id" : "请求ID，短期内唯一",
    "invoke" : "请求接口名",
    "data" : "接口需要的数据"
}
```
对于接口需要的数据，同样需要使用JSON格式序列化数据，例如接口需要name和password两个值，那么上述的data中需要包含内容：
```json
{
    "name" : "username",
    "password" : "123456"
}
```
## 使用
### 接口声明
与传统的SpringMVC控制器类似，需要在接口类上添加@com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path注解，其中value值为该类方法请求的根路径，然后在要暴露的函数上同样添加@com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path注解。
### 参数注入
与SpringMVC不同的是，如果方法具有多个参数，需要使用@com.joe.easysocket.server.backserver.mvc.impl.param.GeneralParam注解声明参数名（这点做的不如SpringMVC，spring不需要注解也可以获取到形参名作为参数名，JDK1.8通过开启选项同样可以再运行时获取到形参名）。
### Context注入
与GeneralParam注解不同的是，如果需要注入Session、RequestWrapper，那么需要使用该注解而不是GeneralParam注解。

## 示例
### 声明一个用户登录接口
声明一个用户登录接口，需要account和password：
```java
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.GeneralParam;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;

/**
 * @author joe
 */
@Path("user")
public class UserController {
    @Path("login")
    public void login(@GeneralParam("account") String account, @GeneralParam("password") String password, @Context
            Session session) {
        System.out.println("账号是：" + account + "，密码是：" + password);
    }
}
```
该数据对应的数据报body为：
```json
{
    "id" : "123456789",
    "invoke" : "user/login",
    "data" : "{ \"account\" : \"123456\",\"password\" : \"123456\"}"
}
```
需要注意到，data中的account和password名称需要与@GeneralParam("account")和@GeneralParam("password")的value一致。

## resource详细定义
如上所示，可以在类上使用@Path注解来声明该类是一个resource类（和SpringMVC中的controller差不多），然后在每个资源方法上同样使用@Path来声明，与SpringMVC不同的是，该socketMVC不支持参数名推断，也就是如果有多个参数那么必须在参数上使用GeneralParam注解来声明参数名。

默认参数是json格式的，省略了注解@Consumes和注解@Produces，上述例子完整的声明如下：
```java
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.GeneralParam;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Consumes;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Produces;
import com.joe.easysocket.server.common.protocol.ChannelProxy;
import com.joe.utils.concurrent.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author joe
 */
@Path("user")
public class UserController {
    AtomicInteger count = new AtomicInteger(0);

    @Path("login")
    @Consumes("json")
    @Produces("json")
    public String login(@GeneralParam("account") String account, @GeneralParam("password") String password, @Context
            Session session) {
        System.out.println("\n\n\n\n\n\n\n\n账号是：" + account + "，密码是：" + password + "\n\n\n\n\n\n\n\n\n\n\n\n\n");
        Map<String, String> map = new HashMap<>();
        map.put("account", account);
        map.put("password", password);
        System.out.println("\n\n\n\n\n\n\nsession is " + session + "\n\n\n\n\n\n\n");
        session.setAttribute("user", map);
        ChannelProxy channel = session.getChannel();
        new Thread(() -> {
            ThreadUtil.sleep(5);
            channel.write("测试一下", "你好啊，这是一条主动发往客户端的消息");
        }).start();
        return "success";
    }

    @Path("print")
    public void print(@Context Session session) {
        System.out.println("session中用户是:" + session.getAttribute("user"));
    }
}
```
如上代码所示，login resource显式声明了参数使用json处理器解析，响应同样使用json处理器解析，当前系统内置一个json参数/响应处理器com.joe.easysocket.server.backserver.mvc.impl.coder.json.JsonDataRW，该处理器处理json类型的参数/响应，如果没有@Consumes和@Produces注解，则系统默认使用json处理器处理请求参数和响应。

**当前系统默认只支持json类型的参数和响应**

如果想要扩展参数处理器，则只需要像JsonDataRW那样实现DataReader和DataWriter接口，然后在该类上添加@Provider注解即可（需要可以被BeanContainer发现），如果只需要对请求参数解析或者只需要对响应数据处理那么只需要实现其中的一个接口即可。对于使用自定义的解析器，需要在resource上添加@Consumes和@Produces注解，然后指定value值，该值将会传入自定义解析器用于过滤出可用的解析器。

## filter使用
后台[backserver](backserver)内置的[MvcDataworker](backserver/src/main/java/com/joe/easysocket/server/backserver/impl/MvcDataworker)支持filter操作（就像SpringMVC中一样，不过支持没有SpringMVC的强大），使用时只需要继承NioRequestFilter或者NioResponseFilter或者NioFilter，然后在类上添加Provider注解即可。

RequestFilter示例：
```java
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
```java
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

[下一节：集成spring](spring.md)