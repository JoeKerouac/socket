package com.joe.easysocket.server.balance.spi;

import com.joe.easysocket.server.balance.protocol.CloseCause;
import com.joe.easysocket.server.balance.protocol.ProtocolEventListener;

/**
 * 抽象事件中心
 * <p>
 * <strong>数据不应该在监听器中处理</strong>
 *
 * @author joe
 * @version 2018.01.29 11:50
 */
public interface EventCenter {
    /**
     * 注册事件监听器
     *
     * @param listener 事件监听器
     */
    void register(ProtocolEventListener listener);

    /**
     * 当通道注销但是有未读消息时将会触发该方法
     *
     * @param channel 通道ID
     * @param data    未读的消息（不完整）
     */
    void discard(String channel, byte[] data);

    /**
     * 通道正常关闭会触发该方法
     *
     * @param channel    通道ID
     * @param closeCause 关闭原因
     */
    void close(String channel, CloseCause closeCause);


    /**
     * 当有消息从协议栈发送到底层准备发往客户端时会触发该方法
     *
     * @param channel 通道ID
     * @param data    发送的消息
     */
    void send(String channel, byte[] data);

    /**
     * 当有通道注册成功会触发该方法
     *
     * @param channel 通道的ID
     */
    void register(String channel);

    /**
     * 协议栈从下层接收到消息并处理失败
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     * @param e       异常原因
     */
    void receiveError(String channel, byte[] data, Throwable e);

    /**
     * 协议栈从下层接收到消息并处理成功
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    void receiveSuccess(String channel, byte[] data);

    /**
     * 协议栈从下层接收到消息暂未处理
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    void receive(String channel, byte[] data);
}
