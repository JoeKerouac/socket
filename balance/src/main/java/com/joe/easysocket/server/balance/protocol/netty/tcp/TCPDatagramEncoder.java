package com.joe.easysocket.server.balance.protocol.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 编码器，将数据写入缓冲区等待写出
 *
 * @author joe
 */
@Slf4j
@Sharable
public class TCPDatagramEncoder extends MessageToByteEncoder<byte[]> {
    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
        out.writeBytes(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg,
                      ChannelPromise promise) throws Exception {
        log.debug("写出channel数据");
        super.write(ctx, msg, promise);
        // 写完后flush
        ctx.flush();
    }

}
