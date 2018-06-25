package com.joe.test;

import com.joe.easysocket.server.backserver.BackServer;
import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.balance.Balance;
import com.joe.easysocket.server.balance.BalanceImpl;
import com.joe.easysocket.server.balance.protocol.netty.tcp.TCPConnectorManager;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.impl.publish.local.LocalPublishCenter;
import com.joe.easysocket.server.common.spi.impl.registry.local.LocalRegistry;

/**
 * 辅助启动类
 *
 * @author joe
 * @version 2018.06.25 18:18
 */
public class Starter {
    static String host = "192.168.2.119";
    static PublishCenter publishCenter = new LocalPublishCenter();
    static Registry registry = new LocalRegistry();

    /**
     * 启动一个前端
     */
    static void startBalance() {
        try {
            com.joe.easysocket.server.balance.Config config = com.joe.easysocket.server.balance.Config.builder()
                    .connectorManager(TCPConnectorManager.class.getName()).clusterConfig(ClusterConfig.builder()
                            .publishCenter(publishCenter).registry(registry).build()).port(10051).host(host).build();

            Balance balance = new BalanceImpl(config);
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
                    .clusterConfig(ClusterConfig.builder()
                            .registry(registry)
                            .publishCenter(publishCenter).build())
                    .host(host)
                    .name("后端" + System.currentTimeMillis()).build();

            BackServer backServer = BackServer.build(config);
            backServer.start(() -> System.out.println("系统关闭了"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
