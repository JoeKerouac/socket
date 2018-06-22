package com.joe.easysocket.server.balance.protocol.netty.udp;

import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.common.exception.SystemException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author joe
 * @version 2018.06.21 16:30
 */
@Slf4j
public class UDPConnectorManager extends AbstractConnectorManager implements ConnectorManager {
    //当前服务器是否运行，只有调用start才会改变状态
    private AtomicBoolean start = new AtomicBoolean(false);
    // 处理请求的线程组，默认是机器核心的两倍
    private EventLoopGroup workerGroup;

    /**
     * 关闭服务器
     */
    @Override
    public synchronized void shutdown() {
        if (!start.get()) {
            log.debug("服务器已经关闭，请勿重复关闭");
        }
        log.warn("服务器开始关闭................");
        log.debug("开始关闭主线程组");

        log.debug("开始关闭工作线程组");
        workerGroup.shutdownGracefully();
        workerGroup = null;

        super.shutdown();

        log.warn("服务器关闭完成");
        start.set(false);
    }

    /**
     * 启动服务器
     */
    @Override
    public synchronized void start() {
        if (start.get()) {
            log.warn("客户端连接服务器已经在运行中，请勿重复启动");
        }

        super.start();
        try {
            log.info("开始初始化客户端连接服务器");
            //先初始化
            log.debug("开始初始化客户端连接服务器，初始化端口是：{}；是否延迟发送：{}；等待建立连接的队列长度为：{}", port, nodelay, backlog);
            super.start();
            // 初始化服务端
            Bootstrap bootstrap = new Bootstrap();
            UDPDatagramDecoder datagramDecoder = new UDPDatagramDecoder();
            UDPDatagramEncoder datagramEncoder = new UDPDatagramEncoder();

            if (LINUX) {
                log.debug("当前系统 是 linux系统，采用epoll模型");
                workerGroup = new EpollEventLoopGroup();
                bootstrap.channel(EpollDatagramChannel.class);
            } else {
                log.debug("当前系统 不是 linux系统，采用nio模型");
                workerGroup = new NioEventLoopGroup();
                bootstrap.channel(NioDatagramChannel.class);
            }


            // 带child**的方法例如childHandler（）都是对应的worker线程组，不带child的对应的boss线程组
            bootstrap.group(workerGroup).handler(new ChannelInitializer<DatagramChannel>() {
                @Override
                public void initChannel(DatagramChannel ch) throws Exception {
                    // UDP处理链，顺序不能变
                    ch.pipeline().addLast(datagramDecoder, new UDPConnectorAdapter(UDPConnectorManager.this,
                            eventCenter), datagramEncoder);
                }
            }).option(ChannelOption.SO_BACKLOG, backlog).option(ChannelOption.TCP_NODELAY, nodelay);

            bootstrap.bind(port).sync();
            log.info("监听端口是：{}", port);

            log.debug("添加关闭监听");
            log.debug("关闭监听添加完毕");
            log.info("系统启动完成......");
            start.set(true);
        } catch (Exception e) {
            start.set(false);
            throw new SystemException(e);
        }
    }
}
