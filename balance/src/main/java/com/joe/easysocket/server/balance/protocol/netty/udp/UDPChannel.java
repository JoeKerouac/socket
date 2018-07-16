package com.joe.easysocket.server.balance.protocol.netty.udp;

import java.net.InetSocketAddress;

import com.joe.easysocket.server.balance.protocol.netty.NettyChannel;
import com.joe.easysocket.server.common.protocol.ProtocolFuture;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

/**
 * UDP channel
 *
 * @author joe
 * @version 2018.06.22 10:16
 */
public class UDPChannel extends NettyChannel {
    public UDPChannel(Channel channel) {
        super(channel);
    }

    @Override
    public ProtocolFuture write(byte[] data) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(data);
        InetSocketAddress address = new InetSocketAddress(getRemoteHost(), getPort());

        return writeToChannel(new DatagramPacket(byteBuf, address));
    }

    /**
     * 设置port（主要供UDP用）
     *
     * @param port port
     */
    public void setPort(int port) {
        super.port = port;
    }

    /**
     * 设置host（主要用UDP用）
     *
     * @param host host
     */
    public void setRemoteHost(String host) {
        super.host = host;
    }
}
