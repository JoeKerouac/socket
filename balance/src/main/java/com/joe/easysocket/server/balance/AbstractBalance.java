package com.joe.easysocket.server.balance;


import com.joe.easysocket.server.balance.protocol.AbstractConnectorManager;
import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.balance.spi.EventCenterProxy;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.lambda.Function;
import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.easysocket.server.common.msg.ChannelId;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.SpiLoader;
import com.joe.utils.collection.CollectionUtil;
import com.joe.utils.common.ClassUtils;
import com.joe.utils.common.Tools;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.joe.easysocket.server.common.config.Const.*;


/**
 * 前端模型
 *
 * @author joe
 */
@Slf4j
public abstract class AbstractBalance extends EventCenterProxy implements Balance {
    protected Config config;
    protected ClusterConfig clusterConfig;
    /**
     * 环境信息
     */
    protected Environment environment;
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
     * 注册中心
     */
    protected Registry registry;
    /**
     * 所有后端的集合
     */
    protected ConcurrentHashMap<String, BackServer> allServer = new ConcurrentHashMap<>();


    /**
     * 默认构造器
     *
     * @param config 前端配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    public AbstractBalance(Config config) throws ConfigIllegalException {
        checkConfig(config);
        this.environment = build(config);

        this.config = this.environment.get(CONFIG);
        this.clusterConfig = this.environment.get(CLUSTER_CONFIG);
        this.registry = this.environment.get(REGISTRY);
        this.publishCenter = this.environment.get(PUBLISH_CENTER);

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
     * 构建环境信息
     *
     * @param config 用户Config
     * @return 系统环境信息
     */
    private Environment build(Config config) {
        ClusterConfig clusterConfig = config.getClusterConfig();
        Environment environment = new Environment(config.getEnvironment());

        Registry registry = environment.getSafe(REGISTRY, Registry.class);
        if (registry == null) {
            String registryClass = clusterConfig.getRegistry();
            log.info("加载spi[{}]", registryClass);
            registry = SpiLoader.loadSpi(registryClass, config.getEnvironment());
            log.info("spi [{}] 加载完毕", registryClass);
        } else {
            log.info("从环境信息中获取到了registry [{}]", registry);
        }

        PublishCenter publishCenter = environment.getSafe(PUBLISH_CENTER, PublishCenter.class);
        if (publishCenter == null) {
            String publishCenterClass = clusterConfig.getPublishCenter();
            log.info("加载spi[{}]", publishCenterClass);
            publishCenter = SpiLoader.loadSpi(publishCenterClass, config.getEnvironment());
            log.info("spi [{}] 加载完毕", publishCenterClass);
        } else {
            log.info("从环境信息中获取到了publishCenter [{}]", publishCenter);
        }

        List<Serializer> serializers;
        List<String> serializerClassList = clusterConfig.getSerializers();

        if (!CollectionUtil.safeIsEmpty(serializerClassList)) {
            serializers = new ArrayList<>(serializerClassList.size());
            serializerClassList.parallelStream().forEach(serializerClass -> {
                try {
                    log.info("加载spi[{}]", serializerClass);
                    Serializer serializer = SpiLoader.loadSpi(serializerClass, config.getEnvironment());
                    log.info("spi [{}] 加载完毕", serializerClass);
                    serializers.add(serializer);
                } catch (ConfigIllegalException e) {
                    log.warn("序列化器[{}]加载失败，忽略序列化器[{}]", serializerClass, serializerClass, e);
                }
            });
        } else {
            serializers = Collections.emptyList();
        }

        environment.put(Const.CONFIG, config);
        environment.put(Const.CLUSTER_CONFIG, config.getClusterConfig());
        environment.put(Const.REGISTRY, registry);
        environment.put(Const.PUBLISH_CENTER, publishCenter);
        environment.put(Const.SERIALIZER_LIST, serializers);
        return environment;
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
        allServer.values().parallelStream().forEach(server -> {
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
