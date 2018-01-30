package com.joe.easysocket.server.common.registry;

import com.joe.easysocket.server.common.spi.Registry;

/**
 * 注册中心连接状态监听器
 *
 * @author joe
 */
public interface ConnectionStateListener {
    /**
     * 监听连接状态，当连接状态改变时触发该方法
     *
     * @param registry 注册中心
     * @param newState 当前状态
     */
    void stateChanged(Registry registry, ConnectionState newState);
}
