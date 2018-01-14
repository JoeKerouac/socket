package com.joe.easysocket.server.balance.protocol.listener;

import com.joe.easysocket.server.common.data.ProtocolData;

/**
 * 协议栈数据监听
 *
 * @author joe
 */
public interface ProtocolDataListener {
    /**
     * 处理消息
     *
     * @param data 监听到的消息
     */
    void exec(ProtocolData data);
}
