package com.joe.easysocket.server.common.data;

import com.joe.utils.protocol.DatagramUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * socket虚拟通道与实际通道使用的通信模型
 *
 * @author joe
 */
@Data
@NoArgsConstructor
public class ProtocolData {
    private static final byte[] heartbeat = DatagramUtil.build(null, (byte) 0, (byte) 1).getData();
    /**
     * 要发送的数据
     */
    private byte[] data;
    /**
     * 客户端的port
     */
    private int port;
    /**
     * 客户端的IP
     */
    private String host;
    /**
     * 该消息对应的通道
     */
    private String channel;
    /**
     * 该消息创建时间
     */
    private long reqTime;
    /**
     * 该消息响应时间
     */
    private long respTime;

    /**
     * 构造器
     *
     * @param data    数据
     * @param port    客户端端口
     * @param host    客户端IP
     * @param channel 客户端通道ID
     */
    public ProtocolData(byte[] data, int port, String host, String channel, long reqTime, long respTime) {
        this.data = data;
        this.port = port;
        this.host = host;
        this.channel = channel;
        this.reqTime = reqTime;
        this.reqTime = respTime;
    }

    /**
     * 构建一个心跳包
     *
     * @param port    对应的客户端的端口号
     * @param host    对应的客户端IP
     * @param channel 对应的客户端通道ID
     * @return 心跳包
     */
    public static ProtocolData buildHeartbeat(int port, String host, String channel) {
        return new ProtocolData(heartbeat, port, host, channel, System.currentTimeMillis(), System.currentTimeMillis());
    }
}
