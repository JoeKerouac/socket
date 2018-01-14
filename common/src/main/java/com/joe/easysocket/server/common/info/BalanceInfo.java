package com.joe.easysocket.server.common.info;

/**
 * @author joe
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceInfo {
    /**
     * 本机IP
     */
    private String host;
    /**
     * 监听端口
     */
    private int port;
    /**
     * 前端的ID
     */
    private String id;
    /**
     * 接受后端主动发送的消息的topic
     */
    private String topic;
}
