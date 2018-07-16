package com.joe.easysocket.server.balance.protocol.netty.tcp;

import java.util.List;

import com.joe.easysocket.server.balance.protocol.netty.ByteBufRef;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据读取器
 *
 * @author joe
 */
@Slf4j
@Sharable
public class TCPDatagramDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg,
                          List<Object> out) throws Exception {
        log.info("TCP编码器编码数据");
        out.add(new ByteBufRef(msg));
    }
}
