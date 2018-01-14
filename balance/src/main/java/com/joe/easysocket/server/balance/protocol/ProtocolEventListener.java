package com.joe.easysocket.server.balance.protocol;

/**
 * 事件监听器
 *
 * @author joe
 */
public abstract class ProtocolEventListener {
    /**
     * 处理事件
     *
     * @param channel 事件源的通道
     * @param event   事件源
     * @param args    该事件对应的参数
     */
    public void exec(String channel, ProtocolEvent event, Object... args) {

    }

    /**
     * 当通道注销但是有未读消息时将会触发该方法
     *
     * @param channel 通道ID
     * @param data    未读的消息（不完整）
     */
    public void discard(String channel, byte[] data) {

    }

    /**
     * 通道正常关闭会触发该方法
     *
     * @param channel    通道ID
     * @param closeCause 关闭原因
     */
    public void close(String channel, CloseCause closeCause) {

    }


    /**
     * 当有消息从协议栈发送到底层准备发往客户端时会触发该方法
     *
     * @param channel 通道ID
     * @param data    发送的消息
     */
    public void send(String channel, byte[] data) {

    }

    /**
     * 当有通道注册成功会触发该方法
     *
     * @param channel 通道的ID
     */
    public void register(String channel) {

    }

    /**
     * 协议栈从下层接收到消息并处理失败
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     * @param e       异常原因
     */
    public void receiveError(String channel, byte[] data, Throwable e) {

    }

    /**
     * 协议栈从下层接收到消息并处理成功
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    public void receiveSuccess(String channel, byte[] data) {

    }

    /**
     * 协议栈从下层接收到消息暂未处理
     *
     * @param channel 消息来源通道ID
     * @param data    消息
     */
    public void receive(String channel, byte[] data) {

    }
}
