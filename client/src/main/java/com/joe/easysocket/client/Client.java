package com.joe.easysocket.client;

import com.joe.easysocket.client.core.*;
import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;
import com.joe.easysocket.client.exception.NoRequireParamException;
import com.joe.easysocket.client.ext.*;
import lombok.Builder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * 客户端
 *
 * @author joe
 */
public class Client {
    //默认空实现日志对象
    private static final Logger DEFAULT = new Logger() {
        @Override
        public void debug(String msg) {
            System.out.println(msg);
        }

        @Override
        public void info(String msg) {
            System.out.println(msg);
        }

        @Override
        public void warn(String msg) {
            System.out.println(msg);
        }

        @Override
        public void error(String msg) {
            System.out.println(msg);
        }

        @Override
        public void debug(String flag, String msg) {
            System.out.println(flag + ":" + msg);
        }

        @Override
        public void info(String flag, String msg) {
            System.out.println(flag + ":" + msg);
        }

        @Override
        public void warn(String flag, String msg) {
            System.out.println(flag + ":" + msg);
        }

        @Override
        public void error(String flag, String msg) {
            System.out.println(flag + ":" + msg);
        }
    };
    private Reader reader;
    private Writer writer;
    //日志对象
    private Logger logger;
    private Logger proxy;
    private String host;
    private int port;
    private volatile boolean shutdown = true;
    //心跳线程
    private Thread heartbeatThread;
    //最后一次发送数据的时间的时间戳
    private long lastActive;
    //心跳周期，单位为秒
    private long heartbeat;
    //序列化器
    private Serializer serializer;
    //事件中心
    private EventCenter eventCenter;

    @Builder
    private Client(String host, int port, Logger logger, Serializer serializer, int heartbeat) {
        if (host == null || host.trim().isEmpty()) {
            throw new NoRequireParamException("host");
        }
        if (port < 0) {
            throw new IllegalArgumentException("port不能小于等于0");
        }
        if (serializer == null) {
            throw new NoRequireParamException("serializer");
        }

        this.host = host;
        this.port = port;
        this.logger = logger == null ? DEFAULT : logger;
        this.proxy = logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Client.class);
        this.heartbeat = heartbeat > 30 ? heartbeat : 30;
        this.serializer = serializer;
        this.eventCenter = new EventCenter();
    }

    /**
     * 注册监听器
     * @param listener
     * 监听器
     * @return
     * 监听器ID，用于移除使用
     */
    public int register(EventListener listener) {
        return eventCenter.register(listener);
    }

    /**
     * 移除指定ID的监听器
     * @param id
     * 监听器ID
     */
    public void unregister(int id) {
        eventCenter.unregister(id);
    }

    /**
     * 移除所有监听器
     */
    public void unregisterAll() {
        eventCenter.unregisterAll();
    }

    /**
     * 丢弃事件
     *
     * @param event 要丢弃的事件
     * @param args  事件参数
     */
    private void discard(SocketEvent event, Object... args) {
        logger.warn("事件[" + event + "]将被丢弃");
    }

    public synchronized void start() {
        start0(0);
    }

    /**
     * 启动客户端
     *
     * @param invoker 启动调用者，0表示用户主动调用，1表示重新连接系统调用
     * @return 返回true表示启动成功，返回false表示启动失败（重复启动）
     */
    private synchronized boolean start0(int invoker) {
        if (!shutdown && invoker == 0) {
            throw new IllegalThreadStateException("请勿重复启动客户端");
        }

        SocketChannel channel;
        try {
            channel = SocketChannel.open();
            if (!channel.connect(new InetSocketAddress(host, port))) {
                logger.debug("启动失败");
                return false;
            }

            this.lastActive = System.currentTimeMillis();
            this.reader = new Reader(channel, logger, this::reconnect, serializer, this,this.eventCenter);
            this.writer = new Writer(channel.socket().getOutputStream(), logger, serializer, this::reconnect);

            register(new MessageListener() {
                @Override
                public void receive(InterfaceData data) {
                    ack(data.getId().getBytes());
                }

                @Override
                public Serializer getSerializer() {
                    return serializer;
                }
            });

            this.reader.start();
            this.writer.start();
        } catch (IOException e) {
            proxy.error("连接构建时发生异常:" + e);
            if (invoker == 1) {
                heartbeatThread.interrupt();
            }
            return false;
        }

        this.shutdown = false;
        if (invoker == 0) {
            this.heartbeatThread = new Thread(() -> {
                proxy.debug("心跳线程启动");
                try {
                    while (true) {
                        long cycle = (System.currentTimeMillis() - lastActive) / 1000 + 1;
                        if (cycle >= heartbeat) {
                            //发送心跳包
                            proxy.debug("发送心跳包");
                            writer.write(null, null);
                            this.lastActive = System.currentTimeMillis();
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    proxy.info("服务器关闭,心跳线程关闭");
                }
            }, "心跳线程");
            this.heartbeatThread.start();
            eventCenter.listen(SocketEvent.REGISTER, this);
        } else {
            eventCenter.listen(SocketEvent.RECONNECT, this);
        }
        return true;
    }

    /**
     * 发送数据
     *
     * @param invoke 要调用的接口
     * @param data   要发送的数据的序列化
     * @return 发送状态，返回true表示成功
     */
    public boolean write(String invoke, String data) {
        if (shutdown) {
            throw new RuntimeException("客户端尚未开启");
        }
        if (invoke == null || invoke.trim().isEmpty()) {
            throw new IllegalArgumentException("invoke不能为空");
        }
        this.lastActive = System.currentTimeMillis();
        return writer.write(invoke, data);
    }

    private void ack(byte[] id) {
        writer.ack(id);
    }

    /**
     * 获取当前客户端是否关闭
     *
     * @return 返回true表示已经关闭
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * 关闭客户端
     *
     * @return 返回true表示关闭成功，返回false表示当前客户端已经处于关闭状态
     */
    public boolean shutdown() {
        return shutdown0(0);
    }

    /**
     * 关闭客户端
     *
     * @param flag 0表示用户调用，1表示系统重连调用
     * @return 返回true表示关闭成功，返回false表示当前客户端已经处于关闭状态
     */
    private synchronized boolean shutdown0(int flag) {
        if (shutdown) {
            return false;
        }
        //必须先将标志位设置为true，否则会导致死循环（client调用reader和writer的shutdown，然后reader和writer回调该shutdown）
        shutdown = true;

        this.reader.shutdown();
        this.writer.shutdown();
        return true;
    }

    /**
     * 断线重连
     */
    private synchronized void reconnect() {
        if (!shutdown && (this.reader.shutdown() || this.writer.shutdown())) {
            //如果不是用户主动关闭的那么尝试重新连接（shutdown为true表示是用户主动关闭的）
            //同时由于reader和writer两个
            shutdown0(1);
            start0(1);
        }
    }
}
