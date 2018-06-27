package com.joe.easysocket.server.balance;

import com.joe.easysocket.server.balance.protocol.*;
import com.joe.easysocket.server.balance.protocol.listener.ProtocolDataListener;
import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.balance.server.BackServerImpl;
import com.joe.easysocket.server.balance.spi.ConnectorManager;
import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.balance.strategy.LoadStrategy;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.info.BalanceInfo;
import com.joe.easysocket.server.common.lambda.Function;
import com.joe.easysocket.server.common.lambda.Serializer;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.utils.common.ClassUtils;
import com.joe.utils.common.Tools;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * 前端实现
 *
 * @author joe
 */
@Slf4j
public class BaseBalance extends AbstractBalance {
    /**
     * 当前是否开启
     */
    private boolean started = false;
    /**
     * 后端服务注册的根节点
     */
    private final String registryBackServerUrl;
    /**
     * 该前端的注册地址
     */
    private String registryUrl;
    /**
     * 序列化器
     */
    private List<Serializer> serializers;
    /**
     * 真实后端消息监听
     */
    private ProtocolDataListener protocolDataListener;
    /**
     * 后端主动发送的消息的监听
     */
    private CustomMessageListener<DataMsg> customMessageListener;
    /**
     * 负载策略
     */
    private LoadStrategy strategy;
    /**
     * 处理底层连接的server
     */
    private ConnectorManager connectorManager;
    /**
     * 注册中心
     */
    private Registry registry;
    /**
     * 前端接受后端主动发的消息的通道
     */
    private String msgRecTopic;
    /**
     * 前端的注册组名
     */
    private String balanceGroup;
    /**
     * 注册的根目录
     */
    private String registryBase;
    /**
     * 前端IP
     */
    private String host;
    /**
     * 前端监听端口
     */
    private int port;

    /**
     * 默认构造器
     *
     * @param config 前端配置
     * @throws ConfigIllegalException 配置错误时抛出该异常
     */
    public BaseBalance(Config config) throws ConfigIllegalException {
        super(config);
        ClusterConfig clusterConfig = config.getClusterConfig();
        this.registryBackServerUrl = clusterConfig.getRegistryBase() + clusterConfig.getBackServerGroup();
        this.serializers = clusterConfig.getSerializers() == null ? Collections.emptyList() : clusterConfig
                .getSerializers();
        this.protocolDataListener = this::receiveData;
        //接受后端主动发来的消息
        this.customMessageListener = new CustomMessageListener<DataMsg>() {
            @Override
            public void onMessage(byte[] channel, DataMsg message) {
                log.debug("收到后端主动发来的消息{}", message);
                receiveData(message.getData());
            }

            @Override
            public Class<DataMsg> resolveMessageType() {
                return DataMsg.class;
            }
        };
        this.strategy = config.getStrategy();
        this.registry = clusterConfig.getRegistry();

        String connectorManagerClass = config.getConnectorManager();
        try {
            log.debug("初始化ConnectorManager子类[{}]的实例", connectorManagerClass);
            this.connectorManager = (ConnectorManager) ClassUtils.loadClass(connectorManagerClass).newInstance();
        } catch (Exception e) {
            log.error("构造ConnectorManager实例[{}]失败，可能是没有无参数构造器，请为ConnectorManager实现类[{}]增加无参数构造器",
                    connectorManagerClass, connectorManagerClass, e);
            throw new ConfigIllegalException("构造ConnectorManager实例[" + connectorManagerClass +
                    "]失败，可能是没有无参数构造器，请为ConnectorManager实现类[" + connectorManagerClass + "]增加无参数构造器", e);
        }


        String eventCenterClass = config.getEventCenter();
        EventCenter eventCenter;
        try {
            log.debug("初始化EventCenter子类[{}]的实例", eventCenterClass);
            eventCenter = (EventCenter) ClassUtils.loadClass(eventCenterClass).newInstance();
        } catch (Exception e) {
            log.warn("构造EventCenter实例[{}]失败，可能是没有无参数构造器，将采用默认EventCenter[{}]",
                    connectorManagerClass, DefaultEventCenter.class, e);
            eventCenter = new DefaultEventCenter();
        }
        log.debug("设置Balance的EventCenter代理为：[{}]", eventCenter);
        super.setEventCenter(eventCenter);

        log.debug("初始化ConnectorManager");
        connectorManager.init(config, eventCenter);
        log.debug("ConnectorManager初始化完毕");

        log.debug("初始化服务器信息");
        this.msgRecTopic = clusterConfig.getTopic() + "/" + id;
        this.balanceGroup = clusterConfig.getBalanceGroup();
        this.registryBase = clusterConfig.getRegistryBase();
        this.port = config.getPort() <= 0 ? 10051 : config.getPort();
        this.host = config.getHost();
        log.debug("服务器信息初始化完毕");

        //当有通道关闭时发出通知
        this.connectorManager.register(new ProtocolEventListener() {
            /**
             * 当通道注销但是有未读消息时将会触发该方法
             *
             * @param channel 通道ID
             * @param data    未读的消息（不完整）
             */
            @Override
            public void discard(String channel, byte[] data) {
                unregister(channel);
            }

            /**
             * 通道正常关闭会触发该方法
             *
             * @param channel    通道ID
             * @param closeCause 关闭原因
             */
            @Override
            public void close(String channel, CloseCause closeCause) {
                unregister(channel);
            }

            private void unregister(String channel) {
                pub(channel);
            }
        });
    }


    @Override
    public synchronized void start(Function callback) throws SystemException {
        log.debug("启动前端...");
        if (started) {
            log.warn("前端已经启动，请不要重复启动");
            return;
        }


        log.debug("启动注册中心");
        registry.start();
        log.debug("注册中心启动完毕");

        log.debug("注册对后端的数据监听");
        //注册数据监听
        connectorManager.register(data -> {
            log.debug("使用负载均衡策略寻找一个合适的后端处理数据");
            BackServer server = strategy.next();
            log.debug("由后端{}处理数据{}", server.getId(), data);
            server.write(buildData(data));
        });
        log.debug("对后端的数据监听注册完毕");

        registryUrl = registryBase + balanceGroup + "/";

        log.debug("将前端注册到注册中心{}", registryUrl);
        registryUrl = registry.add(registryUrl, new BalanceInfo(host, port, id, msgRecTopic));

        log.debug("前端注册完毕，注册地址为：{}", registryUrl);

        try {
            registry.addListener(registryBackServerUrl, ((registry1, event) -> {
                log.debug("接收到事件{}", event);
                byte[] data = event.getData().getData();
                BackServerInfo info = serialize(data);
                if (info == null) {
                    log.debug("事件解析出来的后端信息为空");
                    return;
                }
                log.debug("解析出来的服务器信息是：{}", data);
                switch (event.getType()) {
                    case NODE_ADDED:
                        log.debug("添加后端服务器节点：{}", event.getData());
                        addServer(info);
                        break;
                    case NODE_REMOVED:
                        log.debug("删除后端服务器节点：{}", event.getData());
                        removeServer(info.getId());
                        break;
                    case NODE_UPDATED:
                        log.debug("更新后端服务器节点：{}", event.getData());
                        updateServer(info.getId(), info);
                        break;
                }
            }));
        } catch (Exception e) {
            log.error("注册中心添加监听器失败", e);
            if (e instanceof SystemException) {
                throw (SystemException) e;
            } else {
                throw new SystemException("注册中心添加监听器失败", e);
            }
        }

        log.debug("注册对后端主动消息的监听");
        publishCenter.register(msgRecTopic, customMessageListener);
        log.debug("对后端主动消息的监听注册完毕");

        super.start(callback);

        log.debug("启动底层socket监听");
        try {
            connectorManager.start();
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException(e);
        }
        log.debug("socket监听启动完毕");

        started = true;
    }

    @Override
    public synchronized void shutdown() throws SystemException {
        log.debug("关闭前端...");
        if (!started) {
            log.warn("前端已经关闭，请勿重复关闭");
            return;
        }

        log.debug("关闭socket监听");
        try {
            connectorManager.shutdown();
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException(e);
        }
        log.debug("socket监听关闭完成");

        log.debug("关闭注册中心");
        try {
            registry.close();
        } catch (IOException e) {
            log.error("注册中心关闭失败", e);
            throw new SystemException(e);
        }
        log.debug("注册中心关闭成功");


        log.debug("关闭所有虚拟后端");
        strategy.clear();
        log.debug("虚拟后端关闭完成");

        publishCenter.unregister(msgRecTopic, customMessageListener);

        super.shutdown();
        started = false;
    }

    /**
     * 根据ProtocolData构建前端与后端沟通需要的DataMsg
     *
     * @param data ProtocolData
     * @return DataMsg
     */
    private DataMsg buildData(ProtocolData data) {
        DataMsg msg = new DataMsg();
        msg.setData(data);
        msg.setId(Tools.createUUID());
        msg.setCreateTime(System.currentTimeMillis());
        msg.setAckTopic(clusterConfig.getMsgAck() + "/" + id);
        msg.setRespTopic(clusterConfig.getMsgResp() + "/" + id);
        msg.setTopic(msgRecTopic);
        msg.setSrc(id);
        return msg;
    }

    /**
     * 添加后端服务信息
     *
     * @param serverInfo 后端服务信息
     */
    private void addServer(BackServerInfo serverInfo) {
        BackServer server = buildServer(serverInfo);
        allServer.put(server.getId(), server);
        strategy.addLoad(server);
    }

    /**
     * 删除后端服务信息
     *
     * @param id 后端服务的ID
     */
    private void removeServer(String id) {
        strategy.removeLoad(id);
        allServer.remove(id);
    }

    /**
     * 更新后端服务信息
     *
     * @param id         后端的ID
     * @param serverInfo 后端服务信息
     */
    private void updateServer(String id, BackServerInfo serverInfo) {
        strategy.update(id, serverInfo);
        allServer.get(id);
        allServer.compute(id, (k, v) -> {
            if (v != null) {
                return v.update(serverInfo);
            } else {
                return v;
            }
        });
    }

    /**
     * 根据后端信息构建虚拟后端
     *
     * @param serverInfo 后端注册信息
     * @return 虚拟后端
     */
    private BackServer buildServer(BackServerInfo serverInfo) {
        log.debug("根据真实后端信息{}构建虚拟后端", serverInfo);
        BackServerImpl server = new BackServerImpl(config, serverInfo, protocolDataListener, id);
        log.debug("启动虚拟后端{}", serverInfo);
        server.start();
        return server;
    }

    /**
     * 接受并处理后台消息（系统接收到后台消息后会传给该函数处理）
     *
     * @param data 后台消息
     */
    private void receiveData(ProtocolData data) {
        connectorManager.write(data);
    }

    /**
     * 序列化数据，将byte数据反序列化为{@link com.joe.easysocket.server.common.info.BackServerInfo}
     *
     * @param data byte数据
     * @return 反序列化后的后端信息
     */
    private BackServerInfo serialize(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            for (Serializer serializer : serializers) {
                if (serializer.readable(BackServerInfo.class)) {
                    return serializer.read(data, BackServerInfo.class);
                }
            }
            return Serializer.DEFAULTSER.read(data, BackServerInfo.class);
        } catch (Exception e) {
            log.error("序列化数据{}出错", data, e);
            return null;
        }
    }
}
