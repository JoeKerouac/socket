package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.utils.protocol.exception.IllegalRequestException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * 缓冲区引用
 *
 * @author joe
 */
@Slf4j
@Data
public class ByteBufRef {
    // 缓冲区中数据
    private final byte[] data;
    // 缓冲区引用
    private final ByteBuf byteBuf;
    /**
     * 该参数只有UDP有
     */
    private DatagramPacket datagramPacket;

    /**
     * 构造参数，供UDP调用
     *
     * @param datagramPacket DatagramPacket
     */
    public ByteBufRef(DatagramPacket datagramPacket) {
        this(datagramPacket.content());
        this.datagramPacket = datagramPacket;
    }

    /**
     * 构造参数，供TCP调用
     *
     * @param byteBuf ByteBuf
     */
    public ByteBufRef(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;

        log.debug("开始从缓冲区读取数据");
        try {
            // 将byte信息从ByteBuf中读取出来
            int size = byteBuf.readableBytes();
            byte[] data = new byte[size];
            byteBuf.readBytes(data);
            // 解析byte信息
            log.debug("数据读取完毕，读取好的数据为：{}", data);
            this.data = data;
        } catch (Throwable e) {
            log.error("非法请求，请求解析错误", e);
            throw new IllegalRequestException(e);
        }
    }
}
