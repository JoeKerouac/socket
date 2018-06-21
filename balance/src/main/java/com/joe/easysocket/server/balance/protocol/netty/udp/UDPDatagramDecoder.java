package com.joe.easysocket.server.balance.protocol.netty.udp;

import com.joe.easysocket.server.balance.protocol.netty.ByteBufRef;
import com.joe.utils.protocol.exception.IllegalRequestException;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;


/**
 * 数据读取器
 *
 * @author joe
 */
@Slf4j
@Sharable
public class UDPDatagramDecoder extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("UDP编码器编码数据");
        if (msg instanceof DatagramPacket) {
            DatagramPacket packet = (DatagramPacket) msg;
            msg = new ByteBufRef(packet.content());
        } else {
            throw new IllegalRequestException("UDP处理器未知类型：" + msg.getClass());
        }
        super.channelRead(ctx, msg);
    }
}
