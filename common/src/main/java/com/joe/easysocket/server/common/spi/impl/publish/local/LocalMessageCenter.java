package com.joe.easysocket.server.common.spi.impl.publish.local;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.MessageCenter;
import com.joe.easysocket.server.common.spi.Serializer;

/**
 * 本地发布中心，不能用于分布式项目，测试使用
 *
 * @author joe
 * @version 2018.04.11 18:37
 */
public class LocalMessageCenter implements MessageCenter {
    private Map<String, CustomMessageListener> listeners = new ConcurrentHashMap<>();
    private Map<CustomMessageListener, String> map       = new ConcurrentHashMap<>();

    @Override
    public <T> void pub(String channel, T message) {
        listeners.forEach((k, v) -> {
            if (k.equals(channel)) {
                v.onMessage(channel.getBytes(), message);
            }
        });
    }

    @Override
    public <T> void register(String channel, CustomMessageListener<T> listener) {
        listeners.put(channel, listener);
        map.put(listener, channel);
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {
        if (map.containsKey(listener)) {
            listeners.remove(map.remove(listener));
        }
    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {
        map.remove(listener);
        listeners.remove(channel);
    }

    @Override
    public void unregister(String channel) {
        if (listeners.containsKey(channel)) {
            map.remove(listeners.remove(channel));
        }
    }

    @Override
    public void register(Serializer serializer) {

    }

    @Override
    public void start() throws SystemException {

    }

    @Override
    public void shutdown() throws SystemException {

    }

    @Override
    public void init(Environment properties) {

    }
}
