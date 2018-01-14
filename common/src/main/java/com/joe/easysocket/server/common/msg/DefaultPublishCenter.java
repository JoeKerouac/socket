package com.joe.easysocket.server.common.msg;

import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.utils.common.StringUtils;
import com.joe.utils.parse.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 默认的发布中心（单机版，无法实现分布式，同时本实现可能会有BUG，仅为测试使用，线上使用请自行实现发布中心！！！）
 *
 * @author joe
 */
public class DefaultPublishCenter implements PublishCenter {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPublishCenter.class);
    private static final Serializer DEFAULTSERIALIZER = new JsonSerializer();
    //监听者列表，key为channel，value为监听者
    private Map<String, Set<InternalMessageListener<?>>> customMessageListeners;
    //监听者反向缓存，其中key为监听者，value为监听通道，该值主要为了方便根据监听者删除用
    private Map<InternalMessageListener<?>, Set<String>> cache;
    private boolean init = false;
    //序列化器
    private Serializer serializer = DEFAULTSERIALIZER;

    public DefaultPublishCenter() {
        start();
    }

    @Override
    public void register(Serializer serializer) {
        this.serializer = serializer == null ? DEFAULTSERIALIZER : serializer;
        InternalMessageListener.serializer = serializer;
    }

    @Override
    public synchronized void start() {
        logger.debug("开始初始化DefaultPublishCenter");
        if (init) {
            logger.debug("DefaultPublishCenter已经初始化，不能重复初始化");
            return;
        }

        customMessageListeners = new ConcurrentHashMap<>();
        cache = new ConcurrentHashMap<>();

        logger.debug("DefaultPublishCenter初始化完成");
    }

    @Override
    public synchronized void shutdown() {
        logger.debug("准备销毁DefaultPublishCenter");
        if (!init) {
            logger.debug("DefaultPublishCenter已经销毁或未初始化，不能销毁");
            return;
        }
        customMessageListeners.clear();
        logger.debug("DefaultPublishCenter销毁完成");
    }

    @Override
    public void pub(String channel, Object message) {
        logger.debug("为渠道{}发布消息：{}", channel, message);
        customMessageListeners.forEach((key, value) -> {
            if (key.equals(channel)) {
                value.forEach(listner -> {
                    listner.onMessage(channel.getBytes(), serializer.write(message));
                });
            }
        });
    }

    @Override
    public <T> void register(String channel, CustomMessageListener<T> listener) {
        logger.debug("为渠道{}注册监听者{}", channel, listener);
        InternalMessageListener<T> messageListener = new InternalMessageListener(listener);


        if (customMessageListeners.containsKey(channel)) {
            customMessageListeners.get(channel).add(messageListener);
        } else {
            Set<InternalMessageListener<?>> listeners = new CopyOnWriteArraySet<>();
            listeners.add(messageListener);
            Set<InternalMessageListener<?>> oldListeners = customMessageListeners.putIfAbsent(channel, listeners);
            if (oldListeners != null) {
                customMessageListeners.get(channel).addAll(oldListeners);
            }
        }

        if (cache.containsKey(messageListener)) {
            cache.get(messageListener).add(channel);
        } else {
            Set<String> channels = new CopyOnWriteArraySet<>();
            channels.add(channel);
            Set<String> old = cache.putIfAbsent(messageListener, channels);

            if (old != null) {
                cache.get(messageListener).addAll(old);
            }
        }
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {
        logger.debug("删除监听者{}所有的监听", listener);
        if (listener == null) {
            return;
        }

        Set<String> keys = cache.get(listener);

        if (keys == null) {
            return;
        }

        keys.forEach(key -> {
            Set<InternalMessageListener<?>> listeners = customMessageListeners.get(key);
            listeners.remove(listener);
        });
        keys.clear();
    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {
        logger.debug("删除渠道{}下的监听者{}", channel, listener);
        if (StringUtils.isEmpty(channel) && listener == null) {
            return;
        }
        if (StringUtils.isEmpty(channel)) {
            //删除该监听者所有监听渠道
            unregister(listener);
        } else if (listener == null) {
            //删除该渠道的所有监听者
            unregister(channel);
        } else {
            //删除该渠道下的指定监听者
            Set<InternalMessageListener<?>> listeners = customMessageListeners.get(channel);
            if (listeners != null) {
                listeners.remove(listener);
            }
            Set<String> channels = cache.get(listener);
            if (channels != null) {
                channels.remove(channel);
            }
        }
    }

    @Override
    public void unregister(String channel) {
        logger.debug("删除渠道{}下的所有监听者", channel);
        if (StringUtils.isEmpty(channel)) {
            logger.warn("要删除的渠道为空");
            return;
        }
        Set<InternalMessageListener<?>> listeners = customMessageListeners.get(channel);
        if (listeners != null) {
            listeners.forEach(listener -> {
                cache.get(listener).remove(channel);
            });
            listeners.clear();
        }
    }

    /**
     * 实际的消息监听者
     *
     * @param <T> 消息类型
     */
    private static class InternalMessageListener<T> {
        private static Serializer serializer = DEFAULTSERIALIZER;
        private CustomMessageListener<T> listener;

        private InternalMessageListener(CustomMessageListener<T> listener) {
            this.listener = listener;
        }

        /**
         * 处理消息
         *
         * @param channel 消息的渠道
         * @param message 消息
         */
        public void onMessage(byte[] channel, byte[] message) {

            try {
                T t = serializer.read(message, resolveMessageType());
                logger.debug("收到消息，解析出来的结果为：{}", t);
                listener.onMessage(channel, t);
            } catch (Exception e) {
                logger.warn("解析消息{}时出错，消息类型为：{}", message, resolveMessageType());
            }
        }

        /**
         * 返回消息的类型
         *
         * @return 消息的类型
         */
        public Class<T> resolveMessageType() {
            return listener.resolveMessageType();
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (this == obj || this.listener == obj) {
                return true;
            }

            if (obj instanceof InternalMessageListener) {
                InternalMessageListener internalMessageListener = (InternalMessageListener) obj;
                return this.listener.equals(internalMessageListener.listener);
            } else if (obj instanceof CustomMessageListener) {
                CustomMessageListener listener = (CustomMessageListener) obj;
                return listener.equals(this.listener);
            } else {
                return false;
            }
        }
    }

    /**
     * 默认的json序列化器
     */
    private static class JsonSerializer implements Serializer {
        private static final JsonParser parser = JsonParser.getInstance();

        @Override
        public byte[] write(Object obj) {
            if (obj == null) {
                return null;
            } else if (obj instanceof byte[]) {
                return (byte[]) obj;
            } else if (obj instanceof String) {
                return ((String) obj).getBytes();
            }
            return parser.toJson(obj).getBytes();
        }

        @Override
        public <T> T read(byte[] data, Class<T> clazz) {
            if (data == null || clazz == null) return null;
            return parser.readAsObject(data, clazz);
        }

        @Override
        public boolean writeable(Object obj) {
            return true;
        }

        @Override
        public <T> boolean readable(Class<T> clazz) {
            return true;
        }
    }
}
