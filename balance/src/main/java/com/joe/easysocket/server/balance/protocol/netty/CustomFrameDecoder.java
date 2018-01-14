package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.easysocket.server.common.data.Datagram;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 数据报半包解码器；将字节流解析为正确的数据报流
 *
 * @author joe
 */
public class CustomFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * @param maxFrameLength    数据报最大长度（包含消息head和body）
     * @param lengthFieldOffset 数据报head中长度字段的起始位置（从0开始）
     * @param lengthFieldLength 数据报head中长度字段的长度
     * @param headLength        数据报head的长度
     */
    public CustomFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int headLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, headLength - lengthFieldOffset - lengthFieldLength,
                0);
    }

    /**
     * 版本1时的默认构造，后续可能会变
     */
    public CustomFrameDecoder() {
        this(Datagram.MAX_LENGTH, Datagram.LENOFFSET, Datagram.LENLIMIT, Datagram.HEADER);
    }
}
