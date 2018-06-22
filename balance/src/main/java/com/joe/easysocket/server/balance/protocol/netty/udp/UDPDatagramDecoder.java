package com.joe.easysocket.server.balance.protocol.netty.udp;

import com.joe.easysocket.server.balance.protocol.netty.ByteBufRef;
import com.joe.utils.protocol.exception.IllegalRequestException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;


/**
 * 数据读取器
 *
 * @author joe
 */
@Slf4j
@Sharable
public class UDPDatagramDecoder extends SimpleChannelInboundHandler<DatagramPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        log.debug("UDP编码器编码数据");
        super.channelRead(ctx, new ByteBufRef(msg));
    }
}
