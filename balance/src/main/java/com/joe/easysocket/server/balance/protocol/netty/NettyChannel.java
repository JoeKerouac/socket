package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.easysocket.server.common.protocol.PChannel;
import com.joe.easysocket.server.common.protocol.ProtocolFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.NonNull;

import java.net.InetSocketAddress;

/**
 * netty的channel
 *
 * @author joe
 */
public abstract class NettyChannel implements PChannel {
    //实际netty的channel
    protected Channel channel;
    // 最后一次活动时间
    protected long lastActive;
    //channel的ID
    protected String id;
    /**
     * port
     */
    protected int port = -1;
    /**
     * host
     */
    protected String host;

    public NettyChannel(@NonNull Channel channel) {
        this.channel = channel;
        this.id = channel.id().asLongText();
        this.lastActive = System.currentTimeMillis();
    }

    /**
     * 将数据不进行任何处理写入channel
     *
     * @param data 数据
     * @return 写入状态
     */
    public ProtocolFuture writeToChannel(Object data) {
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
