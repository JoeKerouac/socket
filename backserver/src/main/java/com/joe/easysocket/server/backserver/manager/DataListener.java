package com.joe.easysocket.server.backserver.manager;

import com.joe.easysocket.server.common.protocol.ProtocolFuture;

/**
 * 数据监听器
 *
 * @author joe
 */
public interface DataListener {
    /**
     * 数据监听器
     *
     * @param data    数据（实际发送的数据，会利用这个数据构建一个数据报）
     * @param topic   数据要发送到的topic
     * @param channel 对应的底层通道
     * @return 返回一个数据处理的结果
     */
    ProtocolFuture write(byte[] data, String topic, String channel);
}
