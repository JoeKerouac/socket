# 配置
Balance可以通过[com.joe.easysocket.server.balance.Config](balance/src/main/java/com/joe/easysocket/server/balance/Config.java)来配置，而BackServer则是可以通过[com.joe.easysocket.server.backserver.Config](backserver/src/main/java/com/joe/easysocket/server/backserver/Config.java)来配置
# 默认MvcDataworker使用：
## 数据
MvcDataworker数据报的类型（type）为1，body的序列化方式为JSON，详情如下：
```
{
    "id" : "请求ID，短期内唯一",
    "invoke" : "请求接口名",
    "data" : "接口需要的数据"
}
```
对于接口需要的数据，同样需要使用JSON格式序列化数据，例如接口需要name和password两个值，那么上述的data中需要包含内容：
```
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
```
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
    public void login(@GeneralParam("account") String account, @GeneralParam("password") String password, @Context
            Session session) {
        System.out.println("账号是：" + account + "，密码是：" + password);
    }
}
```
该数据对应的数据报body为：
```
{
    "id" : "123456789",
    "invoke" : "user/login",
    "data" : "{
            \"account\" : \"123456\",
            \"password\" : \"123456\"
        }"
}
```
需要注意到，data中的account和password名称需要与@GeneralParam("account")和@GeneralParam("password")的value一致。