package com.joe.easysocket.server.balance.protocol.netty.udp;

import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.protocol.netty.ByteBufRef;
import com.joe.easysocket.server.balance.protocol.netty.ConnectorAdapter;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.common.exception.SystemException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * UDP连接注册、注销管理，数据编码后的最终处理
 *
 * @author joe
 * @version 2018.06.22 10:20
 */
@Slf4j
public class UDPConnectorAdapter extends ConnectorAdapter {

    public UDPConnectorAdapter(ConnectorManager connectorManager, EventCenter eventCenter) {
        super(connectorManager, eventCenter);
    }

    /**
     * 管理通道的注册
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // 注册IO通道
        ((AbstractConnectorManager) connectorManager).register(new UDPChannel(ctx.channel()));
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
            DatagramPacket datagramPacket = ref.getDatagramPacket();
            if (datagramPacket != null) {
                log.debug("当前使用的是UDP，手动设置channel的port和host");
                UDPChannel channel = (UDPChannel) connectorManager.getChannel(id);
                InetSocketAddress address = datagramPacket.sender();
                channel.setPort(address.getPort());
                channel.setRemoteHost(address.getHostString());
            }
        } else {
            throw new SystemException("数据[" + msg + "]类型未知：" + msg.getClass());
        }

        ((AbstractConnectorManager) connectorManager).receive(data, id);
    }
}
