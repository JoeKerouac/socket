package com.joe.easysocket.server.common.spi.impl.publish.redis;

import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.utils.cluster.ClusterManager;
import com.joe.utils.cluster.redis.RedisBaseConfig;
import com.joe.utils.cluster.redis.RedisClusterManagerFactory;
import com.joe.utils.common.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * redis pub/sub模型
 *
 * @author joe
 */
@Slf4j
public class RedisPublishCenter implements PublishCenter {
    private ClusterManager clusterManager;
    /**
     * listener与topic的映射，用于根据listener查找topic
     */
    private final Map<CustomMessageListener<?>, List<String>> listenerStringMap;
    /**
     * listener与ID的映射
     */
    private final Map<ID, Integer> listenerId;
    /**
     * topic与listener的映射，用于根据topic查找listener
     */
    private final Map<String, List<CustomMessageListener<?>>> listenerMap;

    /**
     * 利用完整配置构建发布中心
     *
     * @param config 完整配置
     */
    public RedisPublishCenter(RedisBaseConfig config) {
        this.clusterManager = RedisClusterManagerFactory.getInstance(config);
        this.listenerStringMap = new ConcurrentHashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
        this.listenerId = new ConcurrentHashMap<>();
    }

    /**
     * 使用端口号和主机地址快速构建发布中心
     *
     * @param host 主机地址
     * @param port 端口
     */
    public RedisPublishCenter(String host, int port) {
        this(RedisClusterManagerFactory.buildRedisConfig(host, port, null));
    }

    @Override
    public <T> void pub(String channel, T message) {
        clusterManager.<T>getTopic(channel).publish(message);
    }

    @Override
    public <T> void register(String channel, CustomMessageListener<T> listener) {
        if (StringUtils.isEmpty(channel)) {
            log.warn("channel不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.register]channel不能为空");
        }
        if (listener == null) {
            log.warn("listener不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.register]listener不能为空");
        }

        if (!listenerStringMap.containsKey(listener)) {
            listenerStringMap.putIfAbsent(listener, new CopyOnWriteArrayList<>());
        }
        listenerStringMap.get(listener).add(channel);

        if (!listenerMap.containsKey(channel)) {
            listenerMap.putIfAbsent(channel, new CopyOnWriteArrayList<>());
        }
        listenerMap.get(channel).add(listener);

        clusterManager.<T>getTopic(channel).addListener((s, t) -> listener.onMessage(channel.getBytes(), t));
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {
        if (listener == null) {
            log.warn("listener不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.unregister]listener不能为空");
        }

        List<String> channels = listenerStringMap.get(listener);
        if (channels != null && !channels.isEmpty()) {
            channels.forEach(channel -> unregister(channel, listener));
        }
    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {
        if (StringUtils.isEmpty(channel)) {
            unregister(listener);
        } else if (listener == null) {
            unregister(channel);
        }

        List<CustomMessageListener<?>> listeners = listenerMap.get(channel);
        if (listeners != null) {
            listeners.remove(listener);
        }

        List<String> channels = listenerStringMap.get(listener);
        if (channels != null) {
            channels.remove(channel);
        }

        Integer id = listenerId.remove(new ID(channel, listener));
        if (id != null) {
            clusterManager.<T>getTopic(channel).removeListener(id);
        }
    }

    @Override
    public void unregister(String channel) {
        if (StringUtils.isEmpty(channel)) {
            log.warn("channel不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.unregister]channel不能为空");
        }

        List<CustomMessageListener<?>> listeners = listenerMap.get(channel);
        if (listeners != null && !listeners.isEmpty()) {
            listeners.forEach(listener -> unregister(channel, listener));
        }
    }

    @Override
    public void register(Serializer serializer) {
        //该实现不需要此方法
    }

    @Override
    public void start() throws SystemException {

    }

    @Override
    public void shutdown() throws SystemException {
        listenerMap.values().forEach(listeners -> listeners.forEach(this::unregister));
        listenerMap.clear();
        listenerId.clear();
        listenerStringMap.clear();
        clusterManager.shutdown();
    }

    @Data
    @AllArgsConstructor
    public static class ID {
        private String channel;
        private CustomMessageListener<?> listener;
    }
}
