package com.joe.easysocket.server.common.msg;

/**
 * pub消息确认监听（当sub端收到后会发布一个该消息）
 *
 * @author joe
 */
public interface PubMsgListener {
    /**
     * 消息ACK监听
     *
     * @param topic 消息的topic
     * @param id    消息的ID
     */
    void listen(String topic, String id);
}
