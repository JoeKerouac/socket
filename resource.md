## 声明resource（MVC）
该框架提供了一个MVC的后端，可以像springMVC一样声明resource（Controller），声明方法如下：
```java
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.GeneralParam;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
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

[下一节：filter使用](filter.md)