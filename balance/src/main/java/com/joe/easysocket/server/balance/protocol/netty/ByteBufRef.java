package com.joe.easysocket.server.balance.protocol.netty;

import io.netty.buffer.ByteBuf;
import lombok.Data;

/**
 * 缓冲区引用
 *
 * @author joe
 */
@Data
public class ByteBufRef {
    // 缓冲区中数据
    private final byte[] data;
    // 缓冲区引用
    private final ByteBuf byteBuf;

    public ByteBufRef(byte[] data, ByteBuf byteBuf) {
        this.data = data;
        this.byteBuf = byteBuf;
    }
}
