package com.joe.easysocket.server.balance.protocol.netty.udp;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * 编码器，将数据写入缓冲区等待写出
 *
 * @author joe
 */
@Slf4j
@Sharable
public class UDPDatagramEncoder extends MessageToMessageEncoder<DatagramPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        out.add(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.writeAndFlush(msg);
    }
}
