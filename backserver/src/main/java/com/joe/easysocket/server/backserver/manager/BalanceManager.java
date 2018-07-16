package com.joe.easysocket.server.backserver.manager;

import static com.joe.easysocket.server.common.config.Const.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.info.BalanceInfo;
import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.Serializer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author joe
 */
@Slf4j
public class BalanceManager implements Endpoint {
    /**
     * 是否启动
     */
    private volatile boolean          started = false;
    private Map<String, BalanceInfo>  balances;
    /**
     * 前端注册地址
     */
    private String                    registryPath;
    /**
     * 注册中心
     */
    private Registry                  registry;
    /**
     * 序列化器
     */
    private List<Serializer>          serializers;
    /**
     * 前端关闭监听
     */
    private List<BalnceCloseListener> listeners;

    public BalanceManager(Environment environment) {
        ClusterConfig clusterConfig = environment.get(CLUSTER_CONFIG);
        this.registry = environment.get(REGISTRY);
        this.registryPath = clusterConfig.getRegistryBase() + clusterConfig.getBalanceGroup();
        this.serializers = environment.get(SERIALIZER_LIST);
        if (serializers == null) {
            serializers = Collections.emptyList();
        }
        this.balances = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * 根据前端ID获取前端信息
     *
     * @param id 前端ID
     * @return 前端信息，可能为null
     */
    public BalanceInfo getBalance(String id) {
        return balances.get(id);
    }

    /**
     * 添加前端关闭监听
     *
     * @param balnceCloseListener 前端关闭监听
     */
    public void addCloseListener(BalnceCloseListener balnceCloseListener) {
        listeners.add(balnceCloseListener);
    }

    @Override
    public void start() throws SystemException {
        if (started) {
            log.debug("当前前端管理器已经启动，不能重复启动");
            return;
        }

        try {
            registry.addListener(registryPath, ((registry1, event) -> {
                log.debug("接收到事件{}", event);
                byte[] data = event.getData().getData();
                BalanceInfo info = serialize(data);
                if (info == null) {
                    log.debug("事件解析出来的前端信息为空");
                    return;
                }
                log.debug("解析出来的前端信息是：{}", data);
                switch (event.getType()) {
                    case NODE_ADDED:
                        log.debug("添加前端服务器节点：{}", event.getData());
                        balances.put(info.getId(), info);
                        break;
                    case NODE_REMOVED:
                        log.debug("删除前端服务器节点：{}", event.getData());
                        balances.remove(info.getId());
                        listeners.forEach(listener -> {
                            try {
                                listener.close(info);
                            } catch (Throwable e) {
                                log.error("前端关闭监听{}调用失败", info, e);
                            }
                        });
                        break;
                    case NODE_UPDATED:
                        log.debug("更新前端服务器节点：{}", event.getData());
                        balances.put(info.getId(), info);
                        break;
                }
            }));
        } catch (Exception e) {
            log.error("添加监听器失败", e);
            throw new SystemException(e);
        }

        started = true;
    }

    /**
     * 序列化数据，将byte数据反序列化为{@link com.joe.easysocket.server.common.info.BalanceInfo}
     *
     * @param data byte数据
     * @return 反序列化后的前端信息
     */
    private BalanceInfo serialize(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            for (Serializer serializer : serializers) {
                if (serializer.readable(BalanceInfo.class)) {
                    return serializer.read(data, BalanceInfo.class);
                }
            }
            log.warn("数据[{}]没有对应的序列化器", BalanceInfo.class);
            return null;
        } catch (Exception e) {
            log.error("序列化数据{}出错", data, e);
            return null;
        }
    }

    @Override
    public void shutdown() throws SystemException {
        if (!started) {
            log.debug("当前前端管理器已经关闭，不能重复启动");
            return;
        }
        balances.clear();
        listeners.clear();
        serializers.clear();
        started = false;
    }

    public interface BalnceCloseListener {
        /**
         * 前端关闭
         *
         * @param info 关闭的前端的信息
         */
        void close(BalanceInfo info);
    }
}
