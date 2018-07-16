package com.joe.easysocket.server.backserver.mvc.context;

import com.joe.easysocket.server.common.lambda.Endpoint;

/**
 * session管理器
 *
 * @author joe
 */
public interface SessionManager extends Endpoint {
    /**
     * 获取指定channel信息对应的session，如果没有就创建返回
     *
     * @param channel   客户端通道ID
     * @param balanceId 前端ID
     * @param port      端口信息
     * @param host      客户端的IP
     * @return 指定channel对应的session
     */
    Session get(String channel, String balanceId, int port, String host);

    /**
     * 删除session
     *
     * @param channel 要删除的session对应的通道ID
     * @return 删除的session
     */
    Session remove(String channel);
}
