package com.joe.easysocket.server.common.spi;


import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.msg.CustomMessageListener;

/**
 * 发布中心，用于发布消息和注册消息监听者，序列化器默认是json，需要其他的可以自行注册
 *
 * @author joe
 */
public interface PublishCenter extends Endpoint, Spi {
    /**
     * 发布消息
     *
     * @param channel 渠道
     * @param message 要发布的消息
     */
    <T> void pub(String channel, T message);

    /**
     * 注册消息监听，如果容器已经运行，那么系统会尽快添加该监听（注：该方法添加监听时监听容器肯定已经运行了，所以消息
     * 监听不是实时生效的，但是基本等于实时，除非系统负载过大）
     *
     * @param channel  要监听的渠道
     * @param listener 监听者
     */
    <T> void register(String channel, CustomMessageListener<T> listener);

    /**
     * 移除该监听者对所有消息的监听
     *
     * @param listener 要移除的消息监听者
     */
    <T> void unregister(CustomMessageListener<T> listener);

    /**
     * 移除指定监听者对指定渠道的监听
     *
     * @param channel  指定渠道
     * @param listener 指定监听者
     */
    <T> void unregister(String channel, CustomMessageListener<T> listener);

    /**
     * 移除指定渠道的所有监听者
     *
     * @param channel 指定渠道
     * @throws NullPointerException 当channel为null时抛出该异常
     */
    void unregister(String channel);

    /**
     * 注册序列化器
     *
     * @param serializer 要注册的序列化器
     */
    void register(Serializer serializer);
}
