package com.joe.test.redis;

import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.utils.common.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * redis pub/sub模型
 *
 * @author joe
 */
@Slf4j
public class RedisPublishCenter implements PublishCenter {
    RedissonClient client;
    /**
     * listener与topic的映射，用于根据listener查找topic
     */
    private Map<CustomMessageListener<?>, List<String>> listenerStringMap;
    /**
     * listener与ID的映射
     */
    private Map<ID, Integer> listenerId;
    /**
     * topic与listener的映射，用于根据topic查找listener
     */
    private Map<String, List<CustomMessageListener<?>>> listenerMap;

    public RedisPublishCenter(String host, int port) {
        Config config = new Config();
        config.useSingleServer().setAddress(host + ":" + port);
        this.client = Redisson.create(config);
        this.listenerStringMap = new ConcurrentHashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
        this.listenerId = new ConcurrentHashMap<>();
    }

    @Override
    public <T> void pub(String channel, T message) {
        client.<T>getTopic(channel).publish(message);
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
        synchronized (listenerStringMap) {
            if (listenerStringMap.containsKey(listener)) {
                listenerStringMap.get(listener).add(channel);
            } else {
                List<String> list = new ArrayList<>();
                list.add(channel);
                listenerStringMap.put(listener, list);
            }
        }

        synchronized (listenerMap) {
            if (listenerMap.containsKey(channel)) {
                listenerMap.get(channel).add(listener);
            } else {
                List<CustomMessageListener<?>> list = new ArrayList<>();
                list.add(listener);
                listenerMap.put(channel, list);
            }
        }

        client.<T>getTopic(channel).addListener((s, t) -> listener.onMessage(channel.getBytes(), t));
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {
        if (listener == null) {
            log.warn("listener不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.unregister]listener不能为空");
        }
        synchronized (listenerStringMap) {
            List<String> channels = listenerStringMap.get(listener);
            if (channels != null && !channels.isEmpty()) {
                channels.forEach(channel -> unregister(channel, listener));
            }
        }
    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {
        if (StringUtils.isEmpty(channel)) {
            unregister(listener);
        } else if (listener == null) {
            unregister(channel);
        }

        synchronized (listenerMap) {
            List<CustomMessageListener<?>> listeners = listenerMap.get(channel);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
        synchronized (listenerStringMap) {
            List<String> channels = listenerStringMap.get(listener);
            if (channels != null) {
                channels.remove(channel);
            }
        }
        Integer id = listenerId.remove(new ID(channel, listener));
        if (id != null) {
            client.<T>getTopic(channel).removeListener(id);
        }
    }

    @Override
    public void unregister(String channel) {
        if (StringUtils.isEmpty(channel)) {
            log.warn("channel不能为空");
            throw new IllegalArgumentException("[RedisPublishCenter.unregister]channel不能为空");
        }
        synchronized (listenerMap) {
            List<CustomMessageListener<?>> listeners = listenerMap.get(channel);
            if (listeners != null && !listeners.isEmpty()) {
                listeners.forEach(listener -> unregister(channel, listener));
            }
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

    }

    @Data
    @AllArgsConstructor
    public static class ID {
        private String channel;
        private CustomMessageListener<?> listener;
    }
}
