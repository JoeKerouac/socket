package com.joe.test;

import com.joe.easysocket.server.backserver.BackServer;
import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.balance.AbstractBalance;
import com.joe.easysocket.server.balance.BaseBalance;
import com.joe.easysocket.server.balance.protocol.netty.tcp.TCPConnectorManager;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.spi.impl.publish.local.LocalPublishCenter;
import com.joe.easysocket.server.common.spi.impl.registry.local.LocalRegistry;

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

    static {
        enviroment = new Properties();
        LocalRegistry registry = new LocalRegistry();
        LocalPublishCenter publishCenter = new LocalPublishCenter();
        enviroment.put(Const.REGISTRY, registry);
        enviroment.put(Const.PUBLISH_CENTER, publishCenter);

        clusterConfig = ClusterConfig.builder().build();
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
