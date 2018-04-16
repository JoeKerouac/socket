package com.joe.test;

import com.joe.easysocket.server.backserver.BackServer;
import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.balance.Balance;
import com.joe.easysocket.server.balance.BalanceImpl;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.impl.publish.local.LocalPublishCenter;
import com.joe.easysocket.server.common.spi.impl.registry.local.LocalRegistry;

/**
 * @author joe
 */
public class Test {
    static String host = "192.168.2.71";
    static PublishCenter publishCenter = new LocalPublishCenter();
    static Registry registry = new LocalRegistry();

    public static void main(String[] args) throws Exception {
        new Thread(Test::startBackserver).start();
        Thread.sleep(1000 * 5);
        new Thread(Test::startBalance).start();
    }

    static void startBalance() {
        try {
            com.joe.easysocket.server.balance.Config config = com.joe.easysocket.server.balance.Config.builder()
                    .clusterConfig(ClusterConfig.builder().publishCenter(publishCenter).registry(registry).build())
                    .host(host).build();

            Balance balance = new BalanceImpl(config);
            balance.start(() -> System.out.println("***************服务器关闭了***************"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    static void startBackserver() {
        try {
            Config config = Config.builder().clusterConfig(ClusterConfig.builder().registry(registry).publishCenter
                    (publishCenter).build()).host(host).name("后端" +
                    System.currentTimeMillis()).build();
            BackServer backServer = BackServer.build(config);
            backServer.start(() -> System.out.println("系统关闭了"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
