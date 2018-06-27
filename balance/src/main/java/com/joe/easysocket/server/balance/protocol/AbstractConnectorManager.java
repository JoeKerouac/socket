package com.joe.easysocket.server.balance.protocol;

import com.joe.easysocket.server.balance.Config;
import com.joe.easysocket.server.balance.protocol.listener.ProtocolDataListener;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.balance.spi.EventCenterProxy;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.protocol.PChannel;
import com.joe.utils.common.StringUtils;
import com.joe.utils.concurrent.ThreadUtil;
import com.joe.utils.protocol.Datagram;
import com.joe.utils.protocol.DatagramUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * 协议栈抽象实现（通用实现，同时该实现已经实现了EventCenter，子类只需要将EventCenter的实例传入即可）
 *
 * @author joe
 */
@Slf4j
public abstract class AbstractConnectorManager extends EventCenterProxy implements ConnectorManager {
    // 是否是linux系统
    protected static boolean LINUX;
    /**
     * 当前所有通道，key为链接的ID，value为通道
     */
    private Map<String, PChannel> pChannels;
    /**
     * 线程池，用于处理底层数据
     */
    private ExecutorService service;
    /**
     * 当前协议栈是否销毁，默认销毁
     */
    private volatile ConnectorManagerState state;
    /**
     * 心跳周期，单位：秒
     */
    private int heartbeat;
    /**
     * 心跳超时清理线程，守护线程
     */
    private Thread cleanupThread;
    /**
     * 数据监听器
     */
    private List<ProtocolDataListener> listeners;
    /**
     * 待发送队列
     */
    private Map<String, RetryData> queue;
    /**
     * 发送线程
     */
    private Thread writer;
    /**
     * 协议栈事件中心
     */
    protected EventCenter eventCenter;
    // 监听端口
    protected int port;
    //队列的最大长度
    protected int backlog;
    //是否延迟发送
    protected boolean nodelay;

    static {
        if (System.getProperty("os.name").contains("Linux")) {
            log.debug("当前系统是linux");
            LINUX = true;
        } else {
            log.debug("当前系统是windows");
            LINUX = false;
        }
    }

    /**
     * ConnectorManager状态
     */
    private enum ConnectorManagerState {
        CREATE, INIT, RUNNING, STOP
    }

    public AbstractConnectorManager() {
        this.state = ConnectorManagerState.CREATE;
    }

    @Override
    public void init(Config config, EventCenter eventCenter) {
        if (!changeState(ConnectorManagerState.INIT)) {
            log.warn("当前已经初始化，不能重复初始化，本次将忽略");
            return;
        }
        this.port = config.getPort() <= 0 ? 10051 : config.getPort();
        this.backlog = config.getTcpBacklog() <= 0 ? 512 : config.getTcpBacklog();
        this.nodelay = config.isNodelay();
        if (eventCenter == null || eventCenter == this) {
            log.info("事件中心为空，采用默认事件中心[{}]", DefaultEventCenter.class);
            this.eventCenter = new DefaultEventCenter();
        } else {
            this.eventCenter = eventCenter;
        }

        super.setEventCenter(this.eventCenter);

        this.heartbeat = config.getHeatbeat() < 30 ? 30 : config.getHeatbeat();

        this.listeners = new CopyOnWriteArrayList<>();
        this.pChannels = new ConcurrentHashMap<>();
        this.queue = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        if (!changeState(ConnectorManagerState.RUNNING)) {
            log.warn("协议栈已经初始化，不能重复初始化");
            return;
        }

        log.info("初始化协议栈，心跳周期为：{}秒", heartbeat);

        log.debug("初始化底层数据线程池");
        //初始化信息
        service = ThreadUtil.createPool(ThreadUtil.PoolType.IO, "channel-data-worker");
        log.debug("底层数据线程池初始化完毕");

        this.writer = new Thread(() -> {
            //除非服务器关闭并且没有待发送数据，否则将会一直
            //循环（最多3秒 -> 消息最多重试30次，每次100毫秒，所以是最多3秒）
            while (state == ConnectorManagerState.RUNNING || !queue.isEmpty()) {
                if (!queue.isEmpty()) {
                    log.debug("开始发送需要ACK的数据，本次需要发送{}", queue.size());
                }
                queue.forEach((k, v) -> {
                    log.debug("尝试发送数据{}", v);
                    PChannel channel = pChannels.get(v.getData().getChannel());
                    try {
                        if (v.getRetry() >= 30) {
                            //重试超过30次不再重试
                            log.warn("数据{}重试次数过多，不再重试该数据", v);
                            queue.remove(k);
                        } else if (channel == null) {
                            log.debug("当前通道{}已经不存在，删除数据{}", k, v);
                            queue.remove(k);
                        } else {
                            log.debug("尝试往通道{}发送消息{}", k, v);
                            v.setRetry(v.getRetry() + 1);
                            channel.write(v.getData().getData());
                        }
                    } catch (Throwable e) {
                        log.error("往通道{}发送消息{}时失败，稍后重试", k, v);
                    }
                });
                //等待100毫秒后继续发送
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.warn("数据发送线程被异常中断，忽略该中断", e);
                }
            }

            log.info("数据发送线程关闭，中断心跳过期清理线程");
        }, "data-writer");

        //心跳超时清理线程
        cleanupThread = new Thread(() -> {
            while (state == ConnectorManagerState.RUNNING) {
                log.debug("开始扫描是否有客户端心跳超时");
                long now = System.currentTimeMillis();
                try {
                    pChannels.forEach((id, channel) -> {
                        long last = channel.getLastActive();
                        long circle = (now - last) / 1000;
                        if ((circle - heartbeat) > 0) {
                            log.info("通道{}心跳超时 ", id);
                            //心跳超时，关闭连接
                            close(id, CloseCause.TIMEOUT);
                            pChannels.remove(id);
                        }
                    });
                } catch (Throwable e) {
                    log.error("过期连接清理线程出错", e);
                }
                //每隔1/5的心跳周期检测一次，也就是过期连接最长会延长1/5的心跳周期被清理
                try {
                    Thread.sleep(heartbeat / 5 * 1000);
                } catch (InterruptedException e) {
                    log.warn("心跳线程被异常中断", e);
                }
            }
        }, "heartbeat-clean");
        cleanupThread.setDaemon(true);
        cleanupThread.start();

        this.writer.start();
        log.info("协议栈初始化完成");
    }

    /**
     * 当前协议栈的关闭逻辑：先将标志位destroy设置为true，然后等待数据发送线程关闭，数据发送线程检测到标志位变化
     * 后会自动终止，然后强制中断心跳过期清理线程，心跳过期清理线程被中断后清空协议栈资源占用，然后协议栈关闭完成
     */
    @Override
    public synchronized void shutdown() {
        log.info("开始关闭协议栈");
        if (state == ConnectorManagerState.STOP) {
            log.warn("协议栈已经关闭，请不要重复关闭");
            return;
        } else if (state == ConnectorManagerState.CREATE) {
            throw new IllegalStateException("当前协议栈未启动，不能关闭");
        }

        pChannels.clear();
        listeners.clear();
        service.shutdown();
        log.info("协议栈关闭成功，等待数据发送线程关闭");
    }

    @Override
    public void close(String id, CloseCause cause) {
        log.debug("关闭连接{}，关闭原因为：{}", id, cause);

        if (StringUtils.isEmpty(id)) {
            log.warn("连接ID不能为null");
            return;
        }

        PChannel channel = pChannels.remove(id);
        if (channel == null) {
            log.warn("要关闭的连接{}不存在", id);
            return;
        }
        channel.close();
        eventCenter.close(id, cause);
    }

    @Override
    public void write(ProtocolData data) {
        log.debug("往客户端写数据{}", data);
        Datagram datagram = DatagramUtil.decode(data.getData());

        //无论是否需要ACK，都先直接发送
        PChannel channel = pChannels.get(data.getChannel());
        if (channel == null) {
            log.warn("数据[{}]对应的客户端已经关闭，不能发送" , data);
            return;
        }
        channel.write(data.getData());

        //如果数据需要ACK，那么加入队列等待ACK，如果没有收到那么将继续发送
        if (datagram.ack()) {
            log.debug("数据{}需要ACK，加入队列等待发送", datagram);
            //不是心跳包和响应包，需要ACK，加入队列发送
            addQueue(data, new String(datagram.getId()));
        }
    }

    @Override
    public void register(ProtocolDataListener listener) {
        listeners.add(listener);
    }

    @Override
    public void register(ProtocolEventListener listener) {
        this.eventCenter.register(listener);
    }

    @Override
    public void discard(String channel, byte[] data) {
        this.eventCenter.discard(channel, data);
    }

    @Override
    public void send(String channel, byte[] data) {
        this.eventCenter.send(channel, data);
    }

    @Override
    public void register(String channel) {
        this.eventCenter.register(channel);
    }

    @Override
    public void receiveError(String channel, byte[] data, Throwable e) {
        this.eventCenter.receiveError(channel, data, e);
    }

    @Override
    public void receiveSuccess(String channel, byte[] data) {
        this.eventCenter.receiveSuccess(channel, data);
    }

    @Override
    public void receive(String channel, byte[] data) {
        this.eventCenter.receive(channel, data);
    }

    @Override
    public PChannel getChannel(String id) {
        return pChannels.get(id);
    }

    /**
     * 注册连接
     *
     * @param channel 要注册的链接
     */
    public void register(@NonNull PChannel channel) {
        log.debug("注册ID为：{}的连接{}", channel.id(), channel);
        String id = channel.id();

        if (this.pChannels.containsKey(id) && channel != this.pChannels.get(id)) {
            log.warn("当前连接池中存在id为{}的通道，并且该通道与新通道不是同一个通道，将注销该通道并注册新的通道 ");
            close(id, CloseCause.SYSTEM);
            this.pChannels.put(id, channel);
        } else if (this.pChannels.containsKey(id)) {
            log.warn("通道{}重复注册 ", id);
        } else {
            log.debug("注册通道id为{}的通道{}", id, channel);
            this.pChannels.put(id, channel);
            this.eventCenter.register(channel.id());
        }
    }

    /**
     * 处理底层数据，底层socket通道接收到数据后调用该方法处理
     *
     * @param data 数据
     * @param src  通道ID
     */
    public void receive(byte[] data, String src) {
        log.debug("接收到底层{}传来的数据，开始处理", src);
        eventCenter.receive(src, data);
        service.submit(() -> {
            log.debug("获取[{}]对应的链接", src);
            PChannel channel = this.pChannels.get(src);
            if (channel == null) {
                log.error("接收到来自通道{}的数据{}，但是并未找到对应的通道", src, data);
                return;
            }
            //只要收到消息就心跳一次
            channel.heartbeat();

            ProtocolData protocolData;

            try {
                protocolData = new ProtocolData(data, channel.getPort(), channel.getRemoteHost(), channel.id
                        (), 0, 0);
            } catch (Throwable error) {
                log.error("获取远程信息出错" , error);
                throw new SystemException("获取远程信息出错", error);
            }

            //获取数据报的类型
            byte type = protocolData.getData()[Datagram.TYPE_INDEX];

            if (Datagram.isHeartbeat(type)) {
                log.debug("数据报是心跳包，返回一个心跳包");
                write(ProtocolData.buildHeartbeat(channel.getPort(), channel.getRemoteHost(), src));
            } else if (Datagram.isAck(type)) {
                log.debug("数据报是ACK，处理ACK");
                ack(protocolData);
            } else {
                log.debug("数据不是心跳包，处理数据{}", protocolData);
                try {
                    listeners.forEach(listener -> listener.exec(protocolData));
                } catch (Throwable e) {
                    this.eventCenter.receiveError(src, data, e);
                    log.error("底层传来的数据处理过程中失败，数据为{}", data, e);
                }
            }
        });
    }

    /**
     * 更新当前状态
     *
     * @param state 要更新的状态
     * @return 更新是否成功，如果当前状态和要更新的状态一致返回false，如果当前状态可以转换到要更新的状态那么返回false，否则抛出异常
     * @throws IllegalStateException 如果当前状态不能更新到指定状态则抛出异常
     */
    private synchronized boolean changeState(ConnectorManagerState state) throws IllegalStateException {
        if (this.state == state) {
            return false;
        }

        switch (state) {
            case CREATE:
                this.state = state;
                return true;
            case INIT:
                switch (this.state) {
                    case CREATE:
                        this.state = state;
                        return true;
                    case RUNNING:
                    case STOP:
                        throw new IllegalStateException("now state:" + this.state);
                    default:
                        throw new IllegalStateException("unknown state:" + this.state);
                }
            case RUNNING:
                switch (this.state) {
                    case INIT:
                        this.state = state;
                        return true;
                    case CREATE:
                    case STOP:
                        throw new IllegalStateException("now state:" + this.state);
                    default:
                        throw new IllegalStateException("unknown state:" + this.state);
                }
            case STOP:
                switch (this.state) {
                    case RUNNING:
                        this.state = state;
                        return true;
                    case CREATE:
                    case INIT:
                        throw new IllegalStateException("now state:" + this.state);
                    default:
                        throw new IllegalStateException("unknown state:" + this.state);
                }
            default:
                throw new IllegalStateException("unknown state:" + state);
        }
    }

    /**
     * 处理客户端ACK
     *
     * @param data 客户端响应数据
     */
    private void ack(ProtocolData data) {
        Datagram datagram = DatagramUtil.decode(data.getData());
        String id = new String(datagram.getId());
        log.debug("收到ID为{}的数据报的ACK了", id);
        RetryData old = queue.remove(id);
        log.debug("删除ID为{}的数据报{}", id, old);
    }

    /**
     * 将后端数据加入待发送队列
     *
     * @param data 后端处理后的响应数据
     * @param id   数据ID
     */
    private void addQueue(ProtocolData data, String id) {
        log.debug("将ID为{}的数据报{}加入待发送队列", id, data);
        if (state == ConnectorManagerState.RUNNING) {
            queue.put(id, new RetryData(data));
        } else {
            log.warn("当前连接池已经关闭，不接受发送数据，数据{}:{}将会被抛弃", id, data);
        }
    }

    /**
     * 重试数据
     */
    @Data
    private static class RetryData {
        /**
         * 实际数据
         */
        private final ProtocolData data;
        /**
         * 重试次数
         */
        private int retry;

        public RetryData(ProtocolData data) {
            this.data = data;
            this.retry = 0;
        }
    }
}
