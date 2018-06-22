package com.joe.easysocket.server.balance.protocol.netty.tcp;

import com.joe.easysocket.server.balance.protocol.netty.NettyChannel;
import com.joe.easysocket.server.common.protocol.ProtocolFuture;
import io.netty.channel.Channel;

/**
 * TCP channel
 *
 * @author joe
 * @version 2018.06.22 10:16
 */
public class TCPChannel extends NettyChannel {
    public TCPChannel(Channel channel) {
        super(channel);
    }

    @Override
    public ProtocolFuture write(byte[] data) {
        return writeToChannel(data);
    }
}
