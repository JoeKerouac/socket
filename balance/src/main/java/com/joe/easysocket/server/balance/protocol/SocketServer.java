package com.joe.easysocket.server.balance.protocol;

import com.joe.easysocket.server.balance.protocol.netty.ConnectorAdapter;
import com.joe.easysocket.server.balance.protocol.netty.CustomFrameDecoder;
import com.joe.easysocket.server.balance.protocol.netty.DatagramDecoder;
import com.joe.easysocket.server.balance.protocol.netty.DatagramEncoder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务器
 *
 * @author joe
 */
public interface SocketServer extends ConnectorManager {

    /**
     * 构建默认的netty实现的server
     *
     * @param port        监听端口
     * @param heatbeat    心跳时间，最小30
     * @param isNodelay   是否延迟发送
     * @param backlog     tcp连接队列最大数量
     * @param eventCenter 事件中心
     * @return 使用netty实现的server
     */
    static SocketServer buildDefault(int port, int heatbeat, boolean isNodelay, int backlog, EventCenter eventCenter) {
        return new NettySocketServer(port, heatbeat, isNodelay, backlog, eventCenter);
    }

    /**
     * 默认的基于netty实现的Server
     *
     * @author joe
     */
    class NettySocketServer extends ConnectorManagerImpl implements SocketServer {
        private static final Logger logger = LoggerFactory.getLogger(NettySocketServer.class);
        // 关闭锁
        private static final Object shutdownLock = new Object();
        // 是否是linux系统
        private static boolean linux;
        //当前服务器是否运行，只有调用start才会改变状态
        private AtomicBoolean start = new AtomicBoolean(false);
        // 接受请求的线程组，默认是机器核心的两倍
        private EventLoopGroup mainGroup;
        // 处理请求的线程组，默认是机器核心的两倍
        private EventLoopGroup workerGroup;

        // 监听端口
        private final int port;
        //事件中心
        private final EventCenter eventCenter;
        //队列的最大长度
        private final int backlog;
        //是否延迟发送
        private final boolean nodelay;

        /**
         * netty socket底层连接处理构造器
         *
         * @param port        监听端口
         * @param heatbeat    心跳时间，最小30
         * @param isNodelay   是否延迟发送
         * @param backlog     tcp连接队列最大数量
         * @param eventCenter 事件中心
         */
        private NettySocketServer(int port, int heatbeat, boolean isNodelay, int backlog, EventCenter eventCenter) {
            super(heatbeat, eventCenter);
            this.port = port <= 0 ? 10051 : port;
            this.eventCenter = eventCenter;
            this.backlog = backlog <= 0 ? 512 : backlog;
            this.nodelay = isNodelay;
        }

        static {
            if (System.getProperty("os.name").contains("Linux")) {
                logger.debug("当前系统是linux");
                linux = true;
            } else {
                logger.debug("当前系统是windows");
                linux = false;
            }
        }

        /**
         * 关闭服务器
         */
        @Override
        public synchronized void shutdown() {
            if (!start.get()) {
                logger.debug("服务器已经关闭，请勿重复关闭");
            }
            logger.warn("服务器开始关闭................");
            logger.debug("开始关闭主线程组");
            mainGroup.shutdownGracefully();
            mainGroup = null;

            logger.debug("开始关闭工作线程组");
            workerGroup.shutdownGracefully();
            workerGroup = null;

            super.shutdown();

            logger.warn("服务器关闭完成");
            start.set(false);
        }

        /**
         * 启动服务器
         */
        @Override
        public synchronized void start() {
            if (start.get()) {
                logger.warn("客户端连接服务器已经在运行中，请勿重复启动");
            }

            super.start();
            try {
                logger.info("开始初始化客户端连接服务器");
                //先初始化
                logger.debug("开始初始化客户端连接服务器，初始化端口是：{}；是否延迟发送：{}；等待建立连接的队列长度为：{}", port, nodelay, backlog);
                super.start();
                // 初始化服务端
                ServerBootstrap bootstrap = new ServerBootstrap();
                DatagramDecoder datagramDecoder = new DatagramDecoder();
                DatagramEncoder datagramEncoder = new DatagramEncoder();

                if (linux) {
                    logger.debug("当前系统 是 linux系统，采用epoll模型");
                    mainGroup = new EpollEventLoopGroup();
                    workerGroup = new EpollEventLoopGroup();
                    bootstrap.channel(EpollServerSocketChannel.class);
                } else {
                    logger.debug("当前系统 不是 linux系统，采用nio模型");
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
                                (NettySocketServer.this, eventCenter), datagramEncoder);
                    }
                }).option(ChannelOption.SO_BACKLOG, backlog).childOption(ChannelOption
                        .TCP_NODELAY, nodelay);

                bootstrap.bind(port).sync();
                logger.info("监听端口是：{}", port);

                logger.debug("添加关闭监听");
                logger.debug("关闭监听添加完毕");
                logger.info("系统启动完成......");
                start.set(true);
            } catch (Exception e) {
                start.set(false);
                throw new SystemException(e);
            }
        }
    }
}
