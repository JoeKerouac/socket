package com.joe.easysocket.server.balance.protocol.netty;

import com.joe.easysocket.server.balance.Config;
import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.common.exception.SystemException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ConnectorManager的netty实现类
 *
 * @author joe
 * @version 2018.01.29 15:31
 */
@Slf4j
public class NettyTCPConnectorManager extends AbstractConnectorManager implements ConnectorManager {
    // 是否是linux系统
    private static boolean linux;
    //当前服务器是否运行，只有调用start才会改变状态
    private AtomicBoolean start = new AtomicBoolean(false);
    // 接受请求的线程组，默认是机器核心的两倍
    private EventLoopGroup mainGroup;
    // 处理请求的线程组，默认是机器核心的两倍
    private EventLoopGroup workerGroup;
    // 监听端口
    private int port;
    //队列的最大长度
    private int backlog;
    //是否延迟发送
    private boolean nodelay;

    static {
        if (System.getProperty("os.name").contains("Linux")) {
            log.debug("当前系统是linux");
            linux = true;
        } else {
            log.debug("当前系统是windows");
            linux = false;
        }
    }

    @Override
    public void init(Config config, EventCenter eventCenter) {
        log.debug("初始化NettySocketServer");
        super.init(config, eventCenter);
        this.port = config.getPort() <= 0 ? 10051 : config.getPort();
        this.backlog = config.getTcpBacklog() <= 0 ? 512 : config.getTcpBacklog();
        this.nodelay = config.isNodelay();
        log.debug("NettySocketServer初始化完成");
    }

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
        mainGroup.shutdownGracefully();
        mainGroup = null;

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
            ServerBootstrap bootstrap = new ServerBootstrap();
            DatagramDecoder datagramDecoder = new DatagramDecoder();
            DatagramEncoder datagramEncoder = new DatagramEncoder();

            if (linux) {
                log.debug("当前系统 是 linux系统，采用epoll模型");
                mainGroup = new EpollEventLoopGroup();
                workerGroup = new EpollEventLoopGroup();
                bootstrap.channel(EpollServerSocketChannel.class);
            } else {
                log.debug("当前系统 不是 linux系统，采用nio模型");
                mainGroup = new NioEventLoopGroup();
                workerGroup = new NioEventLoopGroup();
                bootstrap.channel(NioServerSocketChannel.class);
            }

            // 带child**的方法例如childHandler（）都是对应的worker线程组，不带child的对应的boss线程组
            bootstrap.group(mainGroup, workerGroup).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    // 下边的编码解码器顺序不能变，CustomFrameDecoder必须每次都new，其他几个对象不用每次都new但是需要在类上加上@Sharable注解
                    ch.pipeline().addLast(new CustomFrameDecoder(), datagramDecoder, new ConnectorAdapter
                            (NettyTCPConnectorManager.this, eventCenter), datagramEncoder);
                }
            }).option(ChannelOption.SO_BACKLOG, backlog).childOption(ChannelOption
                    .TCP_NODELAY, nodelay);

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
