package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.utils.protocol.exception.IllegalRequestException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 数据读取器
 *
 * @author joe
 */
@Sharable
public class DatagramDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(DatagramDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        byte[] data = this.decode(msg);
        if (data != null) {
            out.add(new ByteBufRef(data, msg));
        }
    }

    private byte[] decode(ByteBuf msg) throws IllegalRequestException {
        logger.debug("开始从缓冲区读取数据");
        try {
            // 将byte信息从ByteBuf中读取出来
            int size = msg.readableBytes();
            byte[] data = new byte[size];
            msg.readBytes(data);
            // 解析byte信息
            logger.debug("数据读取完毕，读取好的数据为：{}", data);
            return data;
        } catch (IllegalRequestException e) {
            logger.error("非法请求，请求解析错误", e);
            throw e;
        }
    }
}
