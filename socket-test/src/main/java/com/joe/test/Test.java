package com.joe.test;

import com.joe.easysocket.server.backserver.BackServer;
import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.impl.MvcDataworker;
import com.joe.easysocket.server.balance.Balance;
import com.joe.easysocket.server.balance.BalanceImpl;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.data.Datagram;
import com.joe.easysocket.server.common.data.DatagramUtil;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.registry.zk.ZKConfig;
import com.joe.easysocket.server.common.registry.zk.ZKRegistry;
import com.joe.test.redis.RedisPublishCenter;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author joe
 */
public class Test {
    static String host = "192.168.1.1";
    static PublishCenter publishCenter = new RedisPublishCenter("192.168.2.222", 7001);
    static String zookeeper = "192.168.2.222:2181";
    static Registry registry = new ZKRegistry(ZKConfig.builder().connectStr(zookeeper).build());

    public static void main(String[] args) throws Exception {
//        new Thread(Test::startBalance).start();
        new Thread(Test::startBackserver).start();
    }

    static void startBalance() {
        try {
            String host = "192.168.2.71";

            com.joe.easysocket.server.balance.Config config = com.joe.easysocket.server.balance.Config.builder()
                    .clusterConfig(ClusterConfig.builder().publishCenter(publishCenter).registry(registry).build())
                    .host(host).build();

            Balance balance = new BalanceImpl(config);
            balance.start(() -> {
                System.out.println("***************服务器关闭了***************");
            });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    static void startBackserver() {
        try {
            Config config = Config.builder().clusterConfig(ClusterConfig.builder().registry(registry).publishCenter
                    (publishCenter).build()).host(host).name("后端" +
                    System.currentTimeMillis()).build();
            config.setDataWorker(new MvcDataworker(config, "123"));
            BackServer backServer = BackServer.build(config);
            backServer.start(() -> {
                System.out.println("系统关闭了");
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
