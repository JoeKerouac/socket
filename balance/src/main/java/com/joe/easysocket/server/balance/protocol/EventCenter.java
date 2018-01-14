package com.joe.easysocket.server.balance.protocol;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件中心
 *
 * @author joe
 */
public class EventCenter {
    private List<ProtocolEventListener> listeners;

    public EventCenter() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * 注册事件监听器
     *
     * @param listener 事件监听器
     */
    public void register(ProtocolEventListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 当通道注销但是有未读消息时将会触发该方法
     *
     * @param channel 通道ID
     * @param data    未读的消息（不完整）
     */
    public void discard(String channel, byte[] data) {
        listeners.forEach(listener -> {
            listener.discard(channel, data);
        });
    }

    /**
     * 通道正常关闭会触发该方法
     *
     * @param channel    通道ID
     * @param closeCause 关闭原因
     */
    public void close(String channel, CloseCause closeCause) {
        listeners.forEach(listener -> {
            listener.close(channel, closeCause);
        });
    }


    /**
     * 当有消息从协议栈发送到底层准备发往客户端时会触发该方法
     *
     * @param channel 通道ID
     * @param data    发送的消息
     */
    public void send(String channel, byte[] data) {
        listeners.forEach(listener -> {
            listener.send(channel, data);
        });
    }

    /**
     * 当有通道注册成功会触发该方法
     *
     * @param channel 通道的ID
     */
    public void register(String channel) {
        listeners.forEach(listener -> {
            listener.register(channel);
        });
    }

    /**
     * 协议栈从下层接收到消息并处理失败
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     * @param e       异常原因
     */
    public void receiveError(String channel, byte[] data, Throwable e) {
        listeners.forEach(listener -> {
            listener.receiveError(channel, data, e);
        });
    }

    /**
     * 协议栈从下层接收到消息并处理成功
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    public void receiveSuccess(String channel, byte[] data) {
        listeners.forEach(listener -> {
            listener.receiveSuccess(channel, data);
        });
    }

    /**
     * 协议栈从下层接收到消息暂未处理
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    public void receive(String channel, byte[] data) {
        listeners.forEach(listener -> {
            listener.receive(channel, data);
        });
    }
}
