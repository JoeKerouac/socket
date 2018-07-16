package com.joe.easysocket.server.common.spi.impl.registry.zk;

import com.joe.easysocket.server.common.spi.ConnectionState;

/**
 * 连接状态监听器
 *
 * @author joe
 */
public interface ZKConnectionStateListener {
    /**
     * 监听连接状态，当连接状态改变时触发该方法
     *
     * @param client   连接的客户端
     * @param newState 当前状态
     */
    void stateChanged(ZKClient client, ConnectionState newState);
}
