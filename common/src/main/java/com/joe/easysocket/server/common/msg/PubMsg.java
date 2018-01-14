package com.joe.easysocket.server.common.msg;

import lombok.Data;

/**
 * 通过pub发送的消息
 *
 * @param <T> 实际的消息类型
 * @author joe
 */
@Data
public class PubMsg<T> {
    /**
     * ID，保证短期内唯一
     */
    private String id;
    /**
     * 前端接收数据的topic
     */
    private String topic;
    /**
     * 消息调用
     */
    private String invoke;
    /**
     * 消息生成时间戳
     */
    private long createTime;
    /**
     * 实际数据
     */
    private T data;
    /**
     * 该消息ack的topic
     */
    private String ackTopic;
    /**
     * 该消息响应的topic
     */
    private String respTopic;
    /**
     * 该消息的来源，前端往后端发的时候是前端ID，后端往前端发的时候是后端ID
     */
    private String src;
}
