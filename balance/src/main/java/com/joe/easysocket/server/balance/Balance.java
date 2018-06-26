package com.joe.easysocket.server.balance;


import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.lambda.Function;
import com.joe.easysocket.server.common.msg.ChannelId;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.utils.common.ClassUtils;
import com.joe.utils.common.Tools;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * 前端模型
 *
 * @author joe
 */
@Slf4j
public abstract class Balance {
    protected Config config;
    protected ClusterConfig clusterConfig;
    /**
     * 该前端的ID，需要全局唯一
     */
    protected final String id;
    /**
     * 启动标志位，true表示已经启动
     */
    private volatile boolean started = false;
    /**
     * 待发通道注销消息，key为通道ID，value为待通知的后端
     */
    private Map<String, List<BackServer>> pubs = new ConcurrentHashMap<>();
    /**
     * 关闭回调
     */
    private Function callback;
    /**
     * 通道注销通知线程
     */
    private Thread channelUnregister;
    /**
     * 发布中心
     */
    protected PublishCenter publishCenter;
    /**
     * 所有后端的集合
     */
    protected CopyOnWriteArrayList<BackServer> allServer = new CopyOnWriteArrayList<>();


    /**
     * 默认构造器
     *
     * @param config 前端配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    public Balance(Config config) throws ConfigIllegalException {
        checkConfig(config);
        this.config = config;
        this.clusterConfig = config.getClusterConfig();
        this.publishCenter = clusterConfig.getPublishCenter();
        this.id = Tools.createUUID();

        this.channelUnregister = new Thread(() -> {
            while (started) {
                pubs.forEach((k, v) -> {
                    if (v.isEmpty()) {
                        log.debug("当前通道{}已经通道注销消息已经通知到了所有后端", k);
                        pubs.remove(k);
                    } else {
                        log.debug("将通道[{}]注销的消息通知到通道列表[{}]", k, v);
                        v.parallelStream().forEach(server -> {
                            BackServer old = getServer(server.getId());
                            BackServerInfo info = server.getServerInfo();

                            if (old == null) {
                                String ackTopic = info.getChannelChangeAckTopic() + "/" + k + "/" + info.getId();
                                log.debug("后端{}已经不存在不在往该后端发送通道注销消息", info.getId());
                                v.remove(server);
                                //取消监听
                                publishCenter.unregister(ackTopic);
                            } else {
                                String topic = info.getChannelChangeTopic() + "/" + info.getId();
                                ChannelId id = new ChannelId(k, this.id);
                                log.debug("通知后端{}通道{}注销", topic, id);
                                publishCenter.pub(topic, id);
                            }
                        });
                    }
                });

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    log.warn("当前通道注销通知线程被异常中断，忽略该中断", e);
                }
            }
            log.info("当前前端已关闭，通道注销线程结束");
        }, "通道注销通知线程");
    }

    /**
     * 接受并处理后台消息（系统接收到后台消息后会传给该函数处理）
     *
     * @param data 后台消息
     */
    abstract void receiveData(ProtocolData data);

    public void start(Function callback) {
        this.started = true;
        this.callback = callback;
        channelUnregister.start();
        addCloseListener();
    }

    /**
     * 关闭前端
     */
    public void shutdown() {
        shutdownCallback();
        this.started = false;
    }

    /**
     * 添加关闭监听，该监听可以监听kill PID，但是对kill -9 PID无效
     */
    private void addCloseListener() {
        // 该关闭监听针对kill PID
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.warn("系统即将关闭");
            shutdown();
            log.debug("系统关闭成功");
        }));
    }

    /**
     * 关闭回调函数
     */
    private void shutdownCallback() {
        try {
            if (callback != null) {
                log.info("系统关闭，调用关闭回调");
                callback.exec();
            }
        } catch (Throwable e) {
            log.error("关闭回调异常", e);
        }
    }

    /**
     * 发送通道关闭广播
     *
     * @param channel 关闭的通道
     */
    protected void pub(String channel) {
        log.debug("通道{}关闭，发送广播", channel);

        //注册监听，加入待发送列表
        allServer.parallelStream().forEach(server -> {
            log.debug("为后端{}添加channel注销消息", server);
            List<BackServer> list = pubs.get(channel);
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                //必须在此处加入，防止父类判断channel对应的list为空时删除该list
                list.add(server);
                list = pubs.putIfAbsent(channel, list);
                if (list != null) {
                    pubs.get(channel).addAll(list);
                }
            } else {
                list.add(server);
            }

            log.debug("注销消息添加完毕，添加ACK监听");

            BackServerInfo info = server.getServerInfo();
            String ackTopic = info.getChannelChangeAckTopic() + "/" + channel + "/" + id + "/" + info.getId();
            publishCenter.register(ackTopic, new
                    CustomMessageListener<String>() {
                        @Override
                        public void onMessage(byte[] channel, String message) {
                            log.debug("接收到服务端{}的响应{}", info.getId(), message);
                            pubs.get(message).remove(server);
                            //取消监听
                            publishCenter.unregister(ackTopic);
                        }

                        @Override
                        public Class<String> resolveMessageType() {
                            return String.class;
                        }
                    });
        });
        log.debug("所有后端的channel注销消息已经加入队列，等待发送");
    }

    /**
     * 从后端列表根据ID获取后端
     *
     * @param id 后端ID
     * @return 对应的后端，不存在时返回null
     */
    private BackServer getServer(String id) {
        for (BackServer server : allServer) {
            if (server.getId().equals(id)) {
                return server;
            }
        }
        return null;
    }


    /**
     * 检查配置的正确性
     *
     * @param config 配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    private void checkConfig(Config config) throws ConfigIllegalException {
        if (config.getConnectorManager() == null) {
            log.info("ConnectorManager未设置，使用默认ConnectorManager[{}]", AbstractConnectorManager.class);
        }
        if (config.getPort() <= 0) {
            log.warn("监听端口号必须大于0");
            throw new ConfigIllegalException("监听端口号必须大于0，请重新设置port");
        } else if (config.getPort() < 1000) {
            log.warn("当前监听端口小于1000，有可能会与系统端口号冲突，建议设置大于1000的值");
        }

        if (config.getTcpBacklog() <= 0) {
            log.warn("tcpBacklog必须大于0");
            throw new ConfigIllegalException("tcpBacklog必须大于0，请设置一个大于0的tcpBacklog");
        }
        if (config.getHeatbeat() <= 30) {
            log.warn("心跳周期不能小于30秒");
            throw new ConfigIllegalException("心跳周期不能小于30秒，请设置一个大于0的heatbeat");
        } else if (config.getHeatbeat() > 500) {
            log.warn("当前心跳周期较长，建议设置300秒左右的心跳周期");
        }
        if (config.getRespTimeout() <= 0) {
            log.warn("响应超时时间必须大于0");
            throw new ConfigIllegalException("响应超时必须大于0，请设置一个大于0的respTimeout");
        } else if (config.getRespTimeout() <= 50) {
            log.warn("当前设置响应超时时间较短，建议加大响应超时时间");
        }
        if (config.getAckTimeout() <= 0) {
            log.warn("ack超时时间必须大于0");
            throw new ConfigIllegalException("ack超时时间必须大于0，请设置一个大于0的ackTimeout");
        } else if (config.getAckTimeout() <= 50) {
            log.warn("当前设置ack超时时间较短，建议加大ack超时时间");
        }

        String connectorManagerClass = config.getConnectorManager();
        try {
            if (!ConnectorManager.class.isAssignableFrom(ClassUtils.loadClass(connectorManagerClass))) {
                throw new ConfigIllegalException("指定的ConnectorManager[" + connectorManagerClass + "]不是" +
                        ConnectorManager.class.getName() + "的子类");
            }
        } catch (ClassNotFoundException e) {
            log.error("指定的ConnectorManager[{}]不存在");
            throw new ConfigIllegalException("指定的ConnectorManager[" + connectorManagerClass + "]不存在", e);
        }

        String eventCenterClass = config.getEventCenter();
        try {
            if (!EventCenter.class.isAssignableFrom(ClassUtils.loadClass(eventCenterClass))) {
                throw new ConfigIllegalException("指定的EventCenter[" + eventCenterClass + "]不是" +
                        EventCenter.class.getName() + "的子类");
            }
        } catch (ClassNotFoundException e) {
            log.error("指定的EventCenter[{}]不存在");
            throw new ConfigIllegalException("指定的EventCenter[" + eventCenterClass + "]不存在", e);
        }
    }
}
