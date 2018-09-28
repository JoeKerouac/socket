# 项目说明
本项目是[easysocket](https://github.com/JoeKerouac/easysocket)项目的升级版，在原有的单机基础上集成了分布式支持，项目分为五部分，分别是：
- [balance](https://github.com/JoeKerouac/socket/tree/master/balance)：balance项目为前端项目，运行在服务器，负责接收、处理、保持客户端连接和往后端服务器分发客户端的数据。
- [backserver](https://github.com/JoeKerouac/socket/tree/master/backserver)：backserver项目为后端服务器项目，运行在服务器，负责处理前端分发过来的客户端数据。
- [common](https://github.com/JoeKerouac/socket/tree/master/common)：common项目为前端项目和后端项目的通用部分。
- [client](https://github.com/JoeKerouac/socket/tree/master/client)：client项目为客户端参考实现，用于测试使用，不建议直接用于生产环境。
- [socket-test](https://github.com/JoeKerouac/socket/tree/master/socket-test)：socket-test项目使用balance项目和backserver项目实现了一个简易的socket服务器，用于给用户提供使用参考。

# Getting Started
## Server端
### Server端组件和简单逻辑
server端包含backserver和balance两个组件，其中balance负责管理客户端的socket连接和数据报的解析，解析后的数据报会交给backserver来处理。
### 构建自己的项目
添加maven依赖如下：
```xml
<dependencies>
    <dependency>
        <groupId>com.github.JoeKerouac</groupId>
        <artifactId>socket-backserver</artifactId>
        <version>1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.JoeKerouac</groupId>
        <artifactId>socket-balance</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

首先要启动一个backserver，用于处理用户发来的数据，然后要启动一个balance，用于处理socket连接和对实际数据处理器backserver的负载均衡，示例代码如下：

首先是一个通用类：
```java
import com.joe.easysocket.server.backserver.BackServer;
import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.balance.AbstractBalance;
import com.joe.easysocket.server.balance.BaseBalance;
import com.joe.easysocket.server.balance.protocol.netty.tcp.TCPConnectorManager;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.spi.impl.publish.local.LocalPublishCenter;
import com.joe.easysocket.server.common.spi.impl.publish.redis.RedisPublishCenter;
import com.joe.easysocket.server.common.spi.impl.registry.local.LocalRegistry;
import com.joe.easysocket.server.common.spi.impl.registry.zk.ZKConfig;
import com.joe.easysocket.server.common.spi.impl.registry.zk.ZKRegistry;
import com.joe.utils.cluster.redis.RedisBaseConfig;
import com.joe.utils.cluster.redis.RedisClusterManagerFactory;

import java.util.Properties;

/**
 * 辅助启动类
 *
 * @author joe
 * @version 2018.06.25 18:18
 */
public class Starter {
    static String host = "192.168.2.119";
    static Properties enviroment;
    static ClusterConfig clusterConfig;
    /**
     * zookeeper连接，请替换为自己的链接
     */
    static String zkConn = "zookeeper.com:2181";
    /**
     * 替换为自己的redis地址
     */
    static String redisHost = "redis.com";
    /**
     * 替换为自己的redis密码，没有就是null
     */
    static String redisPassword = null;
    /**
     * 替换为自己的redis端口
     */
    static int redisPort = 6379;

    /**
     * 使用本地注册中心和PUB/SUB实现，方便快速测试
     */
    static void useLocal() {
        enviroment = new Properties();
        LocalRegistry registry = new LocalRegistry();
        LocalPublishCenter publishCenter = new LocalPublishCenter();
        enviroment.put(Const.REGISTRY, registry);
        enviroment.put(Const.PUBLISH_CENTER, publishCenter);

        clusterConfig = ClusterConfig.builder().build();
    }

    /**
     * 使用redis作为PUB/SUB实现，使用zookeeper作为注册中心
     */
    static void useNet() {
        enviroment = new Properties();

        RedisBaseConfig redisBaseConfig = RedisClusterManagerFactory.buildRedisConfig(redisHost, redisPort, redisPassword);
        enviroment.put(Const.REDIS_CONFIG, redisBaseConfig);
        ZKConfig zkConfig = new ZKConfig();
        zkConfig.setConnectStr(zkConn);
        ZKRegistry registry = new ZKRegistry();
        enviroment.put(Const.REGISTRY, registry);

        clusterConfig = ClusterConfig.builder()
                .registry(ZKRegistry.class.getName())
                .publishCenter(RedisPublishCenter.class.getName())
                .build();
    }


    /**
     * 启动一个前端
     */
    static void startBalance() {
        try {
            com.joe.easysocket.server.balance.Config config = com.joe.easysocket.server.balance.Config.builder()
                    .connectorManager(TCPConnectorManager.class.getName())
                    .clusterConfig(clusterConfig)
                    .port(10051)
                    .host(host)
                    .environment(enviroment)
                    .build();

            AbstractBalance balance = new BaseBalance(config);
            balance.start(() -> System.out.println("***************服务器关闭了***************"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动一个后端
     */
    static void startBackserver() {
        startBackserver(null);
    }

    /**
     * 启动一个后端，并指定BeanContainer
     *
     * @param beanContainer BeanContainer
     */
    static void startBackserver(BeanContainer beanContainer) {
        try {
            Config config = Config.builder()
                    .beanContainer(beanContainer)
                    .clusterConfig(clusterConfig)
                    .host(host)
                    .name("后端" + System.currentTimeMillis())
                    .environment(enviroment)
                    .build();

            BackServer backServer = BackServer.build(config);
            backServer.start(() -> System.out.println("系统关闭了"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
然后是启动类
```java
/**
 * 不依赖于外部系统自启动（spring等）
 *
 * @author joe
 */
public class Test {

    public static void main(String[] args) throws Exception {
        /*
         * 如果要使用redis作为PUB/SUB中心，使用zookeeper作为注册中心的话使用Starter.useNet();代替下边的一行
         */
        Starter.useLocal();
        new Thread(Starter::startBackserver, "backserver").start();
        new Thread(Starter::startBalance, "balance").start();
    }
}
```
这样一个简单的服务器就启动成功了，但是该服务器不能提供服务，因为该服务器还没有实际处理逻辑，下面加一个简单的处理逻辑：
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
    public void login(@GeneralParam("account") String account, @GeneralParam("password") String password, @Context
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
    }

    @Path("print")
    public void print(@Context Session session) {
        System.out.println("session中用户是:" + session.getAttribute("user"));
    }
}
```
这样就有了简单的逻辑处理，需要注意的是上面的UserController必须放到com开头的包下，例如com.UserController，而不能是org.UserController，因为这里没有配置BeanContainer，使用了默认的BeanContainer，默认会扫描com包下的内容。

有了服务端后还需要有客户端来请求，客户端编写参照[客户端](client/README.md)

[下一步](doc/catalog.md)，让我们来完整的开始学习吧！

PS：（本示例代码在[test](socket-test)项目中）

# Future
- 添加后端组件优雅下线功能；
- 后端注册时自动将处理的数据类型注册上，前端接收到数据后自动筛选合适的后端处理；
- 整理容器中的ClassLoader
- 监控/统计

# 联系我
QQ：1213812243

微信：qiao1213812243

添加请注明来源github