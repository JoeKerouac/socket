package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.easysocket.server.common.exception.UnsupportedException;
import com.joe.easysocket.server.common.protocol.PChannel;
import com.joe.easysocket.server.common.protocol.ProtocolFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ServerChannel;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * netty的channel
 *
 * @author joe
 */
public class NettyChannel implements PChannel {
    private Channel channel;
    // 最后一次活动时间
    private long lastActive;
    //channel的ID
    private String id;
    /**
     * port
     */
    private int port = -1;
    /**
     * host
     */
    private String host;

    public NettyChannel(@NonNull Channel channel) {
        this.channel = channel;
        this.id = channel.id().asLongText();
        this.lastActive = System.currentTimeMillis();
    }

    @Override
    public ProtocolFuture write(byte[] data) {
        this.lastActive = System.currentTimeMillis();
        if (!isClosed()) {
            ChannelFuture future = channel.write(data);
            return new ProtocolFuture() {
                @Override
                public boolean isSuccess() {
                    return future.isSuccess();
                }

                @Override
                public boolean isDone() {
                    return future.isDone();
                }
            };
        } else {
            return ProtocolFuture.ERRORFUTURE;
        }
    }

    @Override
    public void heartbeat() {
        this.lastActive = System.currentTimeMillis();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }

        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return channel == null || !channel.isOpen();
    }

    @Override
    public String getRemoteHost() {
        if (host == null) {
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            host = addr.getHostString();
        }
        return host;
    }

    @Override
    public int getPort() {
        if (port < 0) {
            InetSocketAddress addr = (InetSocketAddress) channel.remoteAddress();
            port = addr.getPort();
        }
        return port;
    }

    @Override
    public long getLastActive() {
        return this.lastActive;
    }
}
