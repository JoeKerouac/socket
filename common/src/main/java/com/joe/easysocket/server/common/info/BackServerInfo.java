package com.joe.easysocket.server.common.info;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author joe
 */
@Data
@NoArgsConstructor
public class BackServerInfo {
    /**
     * 后端的ID，全局唯一
     */
    private String id;
    /**
     * 后端名称（可以任意定）
     */
    private String name;
    /**
     * 后端IP
     */
    private String host;
    /**
     * 订阅的消息通道，以/开头，不能以/结尾
     */
    private String topic;
    /**
     * 后端接收到channel注销消息后的acktopic（需要同一组所有后端一致）
     */
    private String channelChangeAckTopic;
    /**
     * channel改变的topic（需要同一组所有后端一致）
     */
    @Builder.Default
    private String channelChangeTopic;
}
