package com.joe.easysocket.server.common.msg;

/**
 * 消息监听者
 *
 * @param <T> 消息类型
 * @author joe
 */
public interface CustomMessageListener<T> {
    /**
     * 处理消息
     *
     * @param channel 消息的渠道
     * @param message 消息
     */
    void onMessage(byte[] channel, T message);

    /**
     * 返回消息的类型
     *
     * @return 消息的类型
     */
    Class<T> resolveMessageType();
}
