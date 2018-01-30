package com.joe.test.redis;

import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.PublishCenter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author joe
 */
public class RedisPublishCenter implements PublishCenter{
    RedissonClient client;
    private Map<CustomMessageListener<?> , String> listenerStringMap;

    public RedisPublishCenter(String host , int port) {
        Config config = new Config();
        config.useSingleServer().setAddress(host + ":" +  port);
        this.client = Redisson.create(config);
        this.listenerStringMap = new ConcurrentHashMap<>();
    }

    @Override
    public <T> void pub(String channel, T message) {
        client.<T>getTopic(channel).publish(message);
    }

    @Override
    public <T> void register(String channel, CustomMessageListener<T> listener) {
        client.<T>getTopic(channel).addListener((s , t) -> {
            listener.onMessage(channel.getBytes() , t);
        });
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {

    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {

    }

    @Override
    public void unregister(String channel) {

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
}
