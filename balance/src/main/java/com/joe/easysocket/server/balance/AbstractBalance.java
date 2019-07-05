package com.joe.easysocket.server.balance;

import static com.joe.easysocket.server.common.config.Const.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.balance.spi.EventCenterProxy;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.lambda.Function;
import com.joe.easysocket.server.common.msg.ChannelId;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.MessageCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.SpiLoader;
import com.joe.utils.common.Tools;
import com.joe.utils.exception.ExceptionWraper;
import com.joe.utils.validator.ValidatorUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 前端模型
 *
 * @author joe
 */
@Slf4j
public abstract class AbstractBalance extends EventCenterProxy implements Balance {

    /**
     * 前端配置
     */
    protected Config                                config;

    /**
     * 分布式节点配置
     */
    protected ClusterConfig                         clusterConfig;

    /**
     * 环境信息
     */
    protected Environment                           environment;

    /**
     * 该前端的ID，需要全局唯一
     */
    protected final String                          id;

    /**
     * 启动标志位，true表示已经启动
     */
    private volatile boolean                        started       = false;

    /**
     * 待发通道注销消息，key为通道ID，value为待通知的后端
     */
    private Map<String, List<BackServer>>           waitBroadcast = new ConcurrentHashMap<>();

    /**
     * 关闭回调
     */
    private Function                                callback;

    /**
     * 通道注销通知线程
     */
    private Thread                                  channelUnregister;

    /**
     * 发布中心
     */
    protected MessageCenter                         messageCenter;

    /**
     * 注册中心
     */
    protected Registry                              registry;

    /**
     * 所有后端的集合
     */
    protected ConcurrentHashMap<String, BackServer> allServer     = new ConcurrentHashMap<>();

    /**
     * 默认构造器
     *
     * @param config 前端配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    public AbstractBalance(Config config) throws ConfigIllegalException {
        checkConfig(config);
        this.environment = Environment.build(config);

        this.config = this.environment.get(CONFIG);
        this.clusterConfig = this.environment.get(CLUSTER_CONFIG);
        this.registry = this.environment.get(REGISTRY);
        this.messageCenter = this.environment.get(MSG_CENTER);

        this.id = Tools.createUUID();

        this.channelUnregister = new Thread(() -> {
            while (started) {
                waitBroadcast.forEach((k, v) -> {
                    if (v.isEmpty()) {
                        log.debug("当前通道{}已经通道注销消息已经通知到了所有后端", k);
                        waitBroadcast.remove(k);
                    } else {
                        log.debug("将通道[{}]注销的消息通知到通道列表[{}]", k, v);
                        v.parallelStream().forEach(server -> {
                            BackServer old = getServer(server.getId());
                            BackServerInfo info = server.getServerInfo();

                            if (old == null) {
                                String ackTopic = info.getChannelChangeAckTopic() + "/" + k + "/"
                                                  + info.getId();
                                log.debug("后端{}已经不存在不在往该后端发送通道注销消息", info.getId());
                                v.remove(server);
                                //取消监听
                                messageCenter.unregister(ackTopic);
                            } else {
                                String topic = info.getChannelChangeTopic() + "/" + info.getId();
                                ChannelId id = new ChannelId(k, this.id);
                                log.debug("通知后端{}通道{}注销", topic, id);
                                messageCenter.pub(topic, id);
                            }
                        });
                    }
                });

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    //该线程正常不会也不应该被中断，如果被中断则将关闭整个前端
                    log.error("当前通道注销通知线程被异常中断，前端将被终止", e);
                    shutdown();
                    throw new RuntimeException("当前通道注销通知线程被异常中断", e);
                }
            }
            log.info("当前前端已关闭，通道注销线程结束");
        }, "通道注销通知线程");
    }

    @Override
    public void start(Function callback) {
        this.started = true;
        this.callback = callback;
        channelUnregister.start();
        addCloseListener();
    }

    @Override
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
    protected void channelCloseBroadcast(String channel) {
        log.debug("通道{}关闭，发送广播", channel);

        //注册监听，加入待发送列表
        allServer.values().parallelStream().forEach(server -> {
            log.debug("为后端{}添加channel注销消息", server);
            List<BackServer> list = waitBroadcast.get(channel);
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                //必须在此处加入，防止父类判断channel对应的list为空时删除该list
                list.add(server);
                list = waitBroadcast.putIfAbsent(channel, list);
                if (list != null) {
                    waitBroadcast.get(channel).addAll(list);
                }
            } else {
                list.add(server);
            }

            log.debug("注销消息添加完毕，添加ACK监听");

            BackServerInfo info = server.getServerInfo();
            String ackTopic = info.getChannelChangeAckTopic() + "/" + channel + "/" + id + "/"
                              + info.getId();
            messageCenter.register(ackTopic, new CustomMessageListener<String>() {
                @Override
                public void onMessage(byte[] channel, String message) {
                    log.debug("接收到服务端{}的响应{}", info.getId(), message);
                    waitBroadcast.get(message).remove(server);
                    //取消监听
                    messageCenter.unregister(ackTopic);
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
        return allServer.get(id);
    }

    /**
     * 检查配置的正确性
     *
     * @param config 配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    private void checkConfig(Config config) throws ConfigIllegalException {
        if (config.getConnectorManager() == null) {
            log.info("ConnectorManager未设置，使用默认ConnectorManager[{}]",
                AbstractConnectorManager.class);
        }

        ExceptionWraper.run(() -> ValidatorUtil.validate(config), ConfigIllegalException::new);
        ExceptionWraper.run(() -> ValidatorUtil.validate(config.getClusterConfig()),
            ConfigIllegalException::new);

        String connectorManagerClass = config.getConnectorManager();
        SpiLoader.loadSpiClass(connectorManagerClass, ConnectorManager.class);

        String eventCenterClass = config.getEventCenter();
        SpiLoader.loadSpiClass(eventCenterClass, EventCenter.class);
    }
}
