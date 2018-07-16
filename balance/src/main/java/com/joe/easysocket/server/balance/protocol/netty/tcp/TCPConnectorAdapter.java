package com.joe.easysocket.server.balance.protocol.netty.tcp;

import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.protocol.netty.ByteBufRef;
import com.joe.easysocket.server.balance.protocol.netty.ConnectorAdapter;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.common.exception.SystemException;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP连接注册、注销管理，数据编码后的最终处理
 *
 * @author joe
 */
@Slf4j
public class TCPConnectorAdapter extends ConnectorAdapter {
    public TCPConnectorAdapter(ConnectorManager connectorManager, EventCenter eventCenter) {
        super(connectorManager, eventCenter);
    }

    /**
     * 管理通道的注册
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // 注册IO通道
        ((AbstractConnectorManager) connectorManager).register(new TCPChannel(ctx.channel()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 从IO通道读取信息
        log.debug("包装请求信息，要包装的信息为：{}", msg);
        String id = ctx.channel().id().asLongText();
        byte[] data;

        //当前只有这一种，包含UDP和TCP
        if (msg instanceof ByteBufRef) {
            ByteBufRef ref = (ByteBufRef) msg;
            data = ref.getData();
            byteBuf = ref.getByteBuf();
        } else {
            throw new SystemException("数据[" + msg + "]类型未知：" + msg.getClass());
        }

        ((AbstractConnectorManager) connectorManager).receive(data, id);
    }
}
