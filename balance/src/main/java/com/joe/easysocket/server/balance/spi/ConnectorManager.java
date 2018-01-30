package com.joe.easysocket.server.balance.spi;

import com.joe.easysocket.server.balance.Config;
import com.joe.easysocket.server.balance.protocol.CloseCause;
import com.joe.easysocket.server.balance.protocol.listener.ProtocolDataListener;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.protocol.PChannel;


/**
 * 连接管理器，该类实现连接的管理（需要能够定期清理心跳超时的客户端）和数据的分发
 * <p>
 * 注意：必须要有默认无参数构造器
 *
 * @author joe
 */
public interface ConnectorManager extends EventCenter, Endpoint {
    /**
     * 初始化，调用该处理器前需要先调用初始化方法
     *
     * @param config      配置
     * @param eventCenter 事件中心，可以为null
     */
    void init(Config config, EventCenter eventCenter);

    /**
     * 协议栈从底层接收数据，然后交由应用层处理（底层接收到数据后可以发送到此处）
     *
     * @param data 底层传过来的数据
     * @param src  数据来源
     */
    void receive(byte[] data, String src);

    /**
     * 关闭指定数据通道（所有需要关闭数据通道的地方必须调用该方法）
     *
     * @param id    通道的ID
     * @param cause 关闭原因
     */
    void close(String id, CloseCause cause);

    /**
     * 注册链接
     *
     * @param channel 要注册的链接
     */
    void register(PChannel channel);

    /**
     * 往连接管理器中的连接发数据
     *
     * @param data 要发送的数据
     */
    void write(ProtocolData data);

    /**
     * 注册数据处理器，当底层通道有消息时会触发该监听
     *
     * @param listener 数据处理器
     */
    void register(ProtocolDataListener listener);
}
