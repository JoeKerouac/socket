package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.easysocket.server.balance.protocol.CloseCause;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象连接管理适配
 *
 * @author joe
 * @version 2018.06.22 10:24
 */
@Slf4j
public abstract class ConnectorAdapter extends ChannelInboundHandlerAdapter {
    //当前连接的读取缓冲
    protected ByteBuf          byteBuf;
    protected ConnectorManager connectorManager;
    protected EventCenter      eventCenter;

    public ConnectorAdapter(ConnectorManager connectorManager, EventCenter eventCenter) {
        this.connectorManager = connectorManager;
        this.eventCenter = eventCenter;
    }

    /**
     * 管理通道的注销，用户正常注销会调用该方法，同时当通道IO异常时也会调用该方法注销通道，用户正常注销时
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("注销通道{}", ctx.channel());
        connectorManager.close(ctx.channel().id().asLongText(), CloseCause.SYSTEM);
    }

    /**
     * IO异常时会调用该方法，不能在此方法内注销IO通道，IO通道会在channelUnRegistered方法中注销
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("连接异常，关闭连接{}", ctx.channel().id().asLongText(), cause);
    }

    /**
     * 通道关闭时会调用该方法（无论是什么情况下关闭的都会调用该方法）
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String id = ctx.channel().id().asLongText();
        log.debug("关闭通道{}", id);
        if (byteBuf == null || byteBuf.readableBytes() < 1) {
            log.debug("通道{}注销，并且该通道没有未读消息", id);
        } else {
            log.info("通道{}注销，并且存在未读消息，发布一个事件", id);
            byte[] discard = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(discard);
            eventCenter.discard(id, discard);
        }
        // 连接断开时
        super.channelInactive(ctx);
    }
}
