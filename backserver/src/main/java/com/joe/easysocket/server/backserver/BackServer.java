package com.joe.easysocket.server.backserver;


import com.joe.easysocket.server.backserver.mvc.DataWorker;
import com.joe.easysocket.server.backserver.mvc.impl.MvcDataworker;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.lambda.Function;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.utils.common.Tools;
import lombok.extern.slf4j.Slf4j;

/**
 * @author joe
 */
public interface BackServer {
    /**
     * 启动后端
     *
     * @param callback 后端关闭回调（收到kill命令时会回调，主动调用shutdown方法也会回调）
     */
    void start(Function callback);

    /**
     * 关闭后端
     */
    void shutdown();

    /**
     * 构建默认的后端
     *
     * @param config 后端配置
     * @return 默认后端
     */
    static BackServer build(Config config) {
        return new BackServerImpl(config);
    }


    @Slf4j
    class BackServerImpl implements BackServer {
        /**
         * 是否启动
         */
        private boolean started = false;
        private String registryPath;
        private Registry registry;
        private String host;
        private String name;
        /**
         * 接收数据的topic
         */
        private String topic;
        private DataWorker dataWorker;
        private Function callback;
        private PublishCenter publishCenter;
        private CustomMessageListener customMessageListener;
        /**
         * 后端的ID，需要全局唯一
         */
        private String id;
        /**
         * 通道注销通知监听topic
         */
        private String channelChangeTopic;
        /**
         * 通道注销消息ack topic
         */
        private String channelChangeAckTopic;

        BackServerImpl(Config config) {
            ClusterConfig clusterConfig = config.getClusterConfig();
            this.id = Tools.createUUID();
            this.registry = clusterConfig.getRegistry();
            this.registryPath = clusterConfig.getRegistryBase() + clusterConfig.getBackServerGroup() + "/";
            this.host = config.getHost();
            this.name = config.getName();
            this.topic = config.getDataSubTopic() + "/" + id;
            this.dataWorker = config.getDataWorker() == null ? new MvcDataworker(config, id) : config.getDataWorker();
            this.publishCenter = clusterConfig.getPublishCenter();
            this.channelChangeAckTopic = clusterConfig.getChannelChangeAckTopic();
            this.channelChangeTopic = clusterConfig.getChannelChangeTopic();
        }

        @Override
        public synchronized void start(Function callback) throws SystemException {
            log.debug("启动后端....");

            if (started) {
                log.warn("后端已经启动，请勿重复启动");
                return;
            }

            log.debug("启动数据处理器");
            dataWorker.start();
            log.debug("数据处理器启动成功");

            log.debug("启动注册中心");
            registry.start();
            log.debug("注册中心启动完毕");
            BackServerInfo info = new BackServerInfo();
            info.setId(id);
            info.setHost(host);
            info.setName(name);
            info.setTopic(topic);
            info.setChannelChangeTopic(this.channelChangeTopic);
            info.setChannelChangeAckTopic(this.channelChangeAckTopic);
            log.debug("注册后端{}到{}", info, registryPath);
            registryPath = registry.add(registryPath, info);
            log.debug("后端{}的注册地址为：{}", info, registryPath);
            log.debug("启动后端数据处理器");
            customMessageListener = new CustomMessageListener<DataMsg>() {
                @Override
                public void onMessage(byte[] channel, DataMsg message) {
                    log.debug("收到前端的消息{}", message);
                    dataWorker.read(message);
                    log.debug("前端的消息{}处理完毕", message);
                }

                @Override
                public Class<DataMsg> resolveMessageType() {
                    return DataMsg.class;
                }
            };
            log.debug("注册数据监听，监听topic：{}", topic);
            publishCenter.register(topic, customMessageListener);
            log.debug("数据监听注册成功");

            this.callback = callback;
            addCloseListener();
            started = true;
        }

        @Override
        public synchronized void shutdown() throws SystemException {
            if (!started) {
                log.debug("当前系统没有启动，不能关闭");
                return;
            }

            //下方关闭顺序暂时不可变，需要先删除注册信息，然后将数据监听关闭，最后等待数据处理器将现有数据处理完然后关闭

            log.debug("删除注册信息{}", registryPath);
            registry.delete(registryPath);
            log.debug("注册信息{}删除成功", registryPath);
            log.debug("删除数据监听");
            publishCenter.unregister(topic, customMessageListener);
            log.debug("数据监听删除完毕");
            log.debug("关闭数据处理器");
            dataWorker.shutdown();
            log.debug("数据处理器关闭完成");

            if (callback != null) {
                try {
                    log.debug("关闭回调不为空，执行关闭回调");
                    callback.exec();
                } catch (Throwable throwable) {
                    log.error("执行关闭回调过程中异常", throwable);
                }
            }
        }

        /**
         * 添加关闭监听，该监听可以监听kill PID，也可以监听System.exit()，但是对kill -9 PID无效
         */
        private void addCloseListener() {
            // 该关闭监听针对kill PID
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.warn("系统监听到关闭信号，即将关闭");
                shutdown();
            }));
        }
    }
}
